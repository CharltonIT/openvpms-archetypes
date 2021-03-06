/*
 * Version: 1.0
 *
 * The contents of this file are subject to the OpenVPMS License Version
 * 1.0 (the 'License'); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.openvpms.org/license/
 *
 * Software distributed under the License is distributed on an 'AS IS' basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * Copyright 2014 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.patient.reminder;

import org.apache.commons.lang.ObjectUtils;
import org.openvpms.archetype.rules.party.ContactArchetypes;
import org.openvpms.archetype.rules.party.Contacts;
import org.openvpms.archetype.rules.party.PurposeMatcher;
import org.openvpms.archetype.rules.party.SMSMatcher;
import org.openvpms.archetype.rules.patient.PatientRules;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.act.DocumentAct;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.lookup.Lookup;
import org.openvpms.component.business.domain.im.party.Contact;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.Constraints;
import org.openvpms.component.system.common.query.IMObjectQueryIterator;
import org.openvpms.component.system.common.query.NamedQuery;
import org.openvpms.component.system.common.query.ObjectSet;
import org.openvpms.component.system.common.query.ObjectSetQueryIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reminder rules.
 *
 * @author Tim Anderson
 */
public class ReminderRules {

    /**
     * The archetype service.
     */
    private final IArchetypeService service;

    /**
     * The patient rules.
     */
    private final PatientRules rules;

    /**
     * The reminder type cache. May be {@code null}.
     */
    private final ReminderTypeCache reminderTypes;

    /**
     * Reminder due indicator.
     */
    public enum DueState {
        NOT_DUE,      // indicates the reminder is in the future, outside the sensitivity period
        DUE,          // indicates the reminder is inside the sensitivity period
        OVERDUE       // indicates the reminder is overdue
    }

    /**
     * The reminder contact classification code.
     */
    private static final String REMINDER = "REMINDER";

    /**
     * Constructs a {@link ReminderRules}.
     *
     * @param service      the archetype service
     * @param patientRules the patient rules
     */
    public ReminderRules(IArchetypeService service, PatientRules patientRules) {
        this(service, null, patientRules);
    }

    /**
     * Constructs a {@link ReminderRules}.
     * <p/>
     * A reminder type cache can be specified to cache reminders. By default, no cache is used.
     *
     * @param service       the archetype service
     * @param reminderTypes a cache for reminder types. If {@code null}, no caching is used
     * @param rules         the patient rules
     */
    public ReminderRules(IArchetypeService service, ReminderTypeCache reminderTypes, PatientRules rules) {
        this.service = service;
        this.rules = rules;
        this.reminderTypes = reminderTypes;
    }

    /**
     * Sets any IN_PROGRESS reminders that have the same patient and matching reminder group and/or type as that in
     * the supplied reminders to COMPLETED.
     * <p/>
     * This only has effect if the reminders have IN_PROGRESS status.
     * <p/>
     * This method should be used in preference to {@link #markMatchingRemindersCompleted(Act)} if multiple reminders
     * are being saved which may contain duplicates. The former won't mark duplicates completed if they are all saved
     * within the same transaction.
     * <p/>
     * Reminders are processed in the order they appear in the list. If later reminders match earlier ones, the later
     * ones will be marked COMPLETED.
     *
     * @param reminders the reminders
     * @throws ArchetypeServiceException for any archetype service exception
     */
    public void markMatchingRemindersCompleted(List<Act> reminders) {
        if (!reminders.isEmpty()) {
            reminders = new ArrayList<Act>(reminders);  // copy it so it can be modified
            while (!reminders.isEmpty()) {
                Act reminder = reminders.remove(0);
                if (ReminderStatus.IN_PROGRESS.equals(reminder.getStatus())) {
                    ActBean bean = new ActBean(reminder, service);
                    ReminderType type = getReminderType(bean);
                    IMObjectReference patient = bean.getNodeParticipantRef("patient");
                    if (type != null && patient != null) {
                        // compare this reminder with the others, to handle matching instances of these first
                        for (Act other : reminders.toArray(new Act[reminders.size()])) {
                            ActBean otherBean = new ActBean(other, service);
                            if (ObjectUtils.equals(patient, otherBean.getNodeParticipantRef("patient"))
                                && hasMatchingTypeOrGroup(other, type)) {
                                markCompleted(other);
                                reminders.remove(other);
                            }
                        }
                    }
                    // now mark any persistent matching reminders completed
                    doMarkMatchingRemindersCompleted(reminder, type, patient);
                }
            }
        }
    }

    /**
     * Sets any IN_PROGRESS reminders that have the same patient and matching reminder group and/or type as that in
     * the supplied reminder to COMPLETED.
     * <p/>
     * This only has effect if the reminder is new and has IN_PROGRESS status.
     * <p/>
     * This method is intended to be invoked just prior to a new reminder being saved.
     *
     * @param reminder the reminder
     * @throws ArchetypeServiceException for any archetype service exception
     */
    public void markMatchingRemindersCompleted(Act reminder) {
        if (reminder.isNew()) {
            doMarkMatchingRemindersCompleted(reminder);
        }
    }

    /**
     * Calculate the due date for a reminder using the reminder's start date
     * plus the default interval and units from the associated reminder type.
     *
     * @param act the act
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void calculateReminderDueDate(Act act) {
        ActBean bean = new ActBean(act, service);
        Date startTime = act.getActivityStartTime();
        ReminderType reminderType = getReminderType(bean);
        Date endTime = null;
        if (startTime != null && reminderType != null) {
            endTime = reminderType.getDueDate(startTime);
        }
        act.setActivityEndTime(endTime);
    }

    /**
     * Calculates the due date for a reminder.
     *
     * @param startTime    the start time
     * @param reminderType the reminder type
     * @return the end time for a reminder
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Date calculateReminderDueDate(Date startTime, Entity reminderType) {
        ReminderType type = new ReminderType(reminderType, service);
        return type.getDueDate(startTime);
    }

    /**
     * Calculates the due date for a product reminder.
     *
     * @param startTime    the start time
     * @param relationship the product reminder relationship
     * @return the due date for the reminder
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Date calculateProductReminderDueDate(Date startTime, EntityRelationship relationship) {
        IMObjectBean bean = new IMObjectBean(relationship, service);
        int period = bean.getInt("period");
        String uom = bean.getString("periodUom");
        return DateRules.getDate(startTime, period, DateUnits.valueOf(uom));
    }

    /**
     * Returns a count of IN_PROGRESS reminders for a patient.
     *
     * @param patient the patient
     * @return the no. of IN_PROGRESS reminders for {@code patient}
     * @throws ArchetypeServiceException for any error
     */
    public int countReminders(Party patient) {
        NamedQuery query = new NamedQuery("act.patientReminder-count",
                                          Arrays.asList("count"));
        query.setParameter("patientId", patient.getId());
        return count(query);
    }

    /**
     * Returns a count of IN_PROGRESS alerts whose endTime is greater than
     * the specified date/time.
     *
     * @param patient the patient
     * @param date    the date/time
     * @return the no. of IN_PROGRESS alerts for {@code patient}
     * @throws ArchetypeServiceException for any error
     */
    public int countAlerts(Party patient, Date date) {
        NamedQuery query = new NamedQuery("act.patientAlert-count",
                                          Arrays.asList("count"));
        query.setParameter("patientId", patient.getId());
        query.setParameter("date", date);
        return count(query);
    }

    /**
     * Determines if a reminder is due in the specified date range.
     *
     * @param reminder the reminder
     * @param from     the 'from' date. May be {@code null}
     * @param to       the 'to' date. Nay be {@code null}
     * @return {@code true} if the reminder is due
     */
    public boolean isDue(Act reminder, Date from, Date to) {
        ActBean bean = new ActBean(reminder, service);
        ReminderType reminderType = getReminderType(bean);
        if (reminderType != null) {
            int reminderCount = bean.getInt("reminderCount");
            return reminderType.isDue(reminder.getActivityEndTime(),
                                      reminderCount, from, to);
        }
        return false;
    }

    /**
     * Determines if a reminder needs to be cancelled, based on its due
     * date and the specified date. Reminders should be cancelled if:
     * <p/>
     * {@code dueDate + (reminderType.cancelInterval * reminderType.cancelUnits) &lt;= date}
     *
     * @param reminder the reminder
     * @param date     the date
     * @return {@code true} if the reminder needs to be cancelled,
     *         otherwise {@code false}
     * @throws ArchetypeServiceException for any archetype service error
     */
    public boolean shouldCancel(Act reminder, Date date) {
        ActBean bean = new ActBean(reminder, service);
        // First check if Patient deceased and if so set to Cancel
        Party patient = (Party) bean.getParticipant("participation.patient");
        EntityBean patientBean = new EntityBean(patient, service);
        if (patientBean.getBoolean("deceased", false)) {
            return true;
        }
        // Otherwise get reminderType and check cancel period
        ReminderType reminderType = getReminderType(bean);
        if (reminderType != null) {
            Date due = reminder.getActivityEndTime();
            return reminderType.shouldCancel(due, date);
        }
        return false;
    }

    /**
     * Cancels a reminder.
     *
     * @param reminder the reminder
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void cancelReminder(Act reminder) {
        ActBean bean = new ActBean(reminder, service);
        bean.setStatus(ReminderStatus.CANCELLED);
        bean.save();
    }

    /**
     * Updates a reminder that has been successfully sent.
     * <p/>
     * This clears the <em>error</em> node.
     *
     * @param reminder the reminder
     * @param lastSent the date when the reminder was sent
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void updateReminder(Act reminder, Date lastSent) {
        ActBean bean = new ActBean(reminder, service);
        int count = bean.getInt("reminderCount");
        updateReminder(reminder, count + 1, lastSent);
    }

    /**
     * Updates a reminder that has been successfully sent.
     * <p/>
     * This clears the <em>error</em> node.
     *
     * @param reminder      the reminder
     * @param reminderCount the reminder count
     * @param lastSent      the date when the reminder was sent
     * @throws ArchetypeServiceException for any archetype service error
     */
    public void updateReminder(Act reminder, int reminderCount, Date lastSent) {
        ActBean bean = new ActBean(reminder, service);
        bean.setValue("reminderCount", reminderCount);
        bean.setValue("lastSent", lastSent);
        bean.setValue("error", null);
        bean.save();
    }

    /**
     * Returns a reminder type template, given the no. of times a reminder
     * has already been sent, and the reminder type.
     *
     * @param reminderCount the no. of times a reminder has been sent
     * @param reminderType  the reminder type
     * @return the corresponding reminder type template, or {@code null}
     *         if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public EntityRelationship getReminderTypeTemplate(int reminderCount,
                                                      Entity reminderType) {
        ReminderType type = new ReminderType(reminderType, service);
        return type.getTemplateRelationship(reminderCount);
    }

    /**
     * Calculates the next due date for a reminder.
     *
     * @param reminder the reminder
     * @return the next due date for the reminder, or {@code null}
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Date getNextDueDate(Act reminder) {
        ActBean bean = new ActBean(reminder, service);
        int count = bean.getInt("reminderCount");
        ReminderType reminderType = getReminderType(bean);
        if (reminderType != null) {
            return reminderType.getNextDueDate(reminder.getActivityEndTime(),
                                               count);
        }
        return null;
    }

    /**
     * Returns the contact for a reminder.
     *
     * @param reminder the reminder
     * @return the contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getContact(Act reminder) {
        Contact contact = null;
        ActBean bean = new ActBean(reminder, service);
        Party patient = (Party) bean.getParticipant("participation.patient");
        if (patient != null) {
            Party owner = rules.getOwner(patient);
            if (owner != null) {
                contact = getContact(owner, reminder);
            }
        }
        return contact;
    }

    /**
     * Returns the contact for a patient owner and reminder.
     *
     * @param owner    the patient owner
     * @param reminder the reminder
     * @return the contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getContact(Party owner, Act reminder) {
        Contact contact;
        ActBean bean = new ActBean(reminder, service);
        ReminderType reminderType = getReminderType(bean);
        EntityRelationship template = null;
        if (reminderType != null) {
            int reminderCount = bean.getInt("reminderCount");
            template = reminderType.getTemplateRelationship(reminderCount);
        }
        if (template != null && template.getTarget() != null) {
            contact = getContact(owner.getContacts());
        } else {
            // no document reminderTypeTemplate, so can't send email or print.
            // Use the customer's phone contact.
            contact = getPhoneContact(owner.getContacts());
        }
        return contact;
    }

    /**
     * Returns a contact for reminders.
     * <p/>
     * This returns:
     * <ol>
     * <li>the first location contact with classification 'REMINDER'; or </li>
     * <li>any contact with classification 'REMINDER'; or</li>
     * <li>the preferred location contact if no contact has a REMINDER classification; or</li>
     * <li>any preferred contact if there is no preferred location contact; or</li>
     * <li>the first available contact if there is no preferred contact.</li>
     * </ol>
     *
     * @param contacts the contacts
     * @return a contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getContact(Set<Contact> contacts) {
        return getContact(contacts, true, ContactArchetypes.LOCATION);
    }
    /**
     * Returns the first contact.location with classification 'REMINDER', or; the
     * preferred contact.location if no contact has this classification,
     * or; the first contact.location if none is preferred.
     *
     * @param contacts the contacts
     * @return a contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getLocationContact(Set<Contact> contacts) {
        return getContact(contacts, false, ContactArchetypes.LOCATION);
    }

    /**
     * Returns the first phone contact with classification 'REMINDER' or
     * the preferred phone contact if no contact has this classification.
     *
     * @param contacts the contacts
     * @return a contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getPhoneContact(Set<Contact> contacts) {
        return getContact(contacts, false, ContactArchetypes.PHONE);
    }

    /**
     * Returns the first SMS phone contact with classification 'REMINDER' or the preferred phone contact if no contact
     * has this classification.
     *
     * @param contacts the contacts
     * @return a contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getSMSContact(Set<Contact> contacts) {
        List<Contact> list = Contacts.sort(contacts); // sort to make it deterministic
        return Contacts.find(list, new SMSMatcher(REMINDER, false, service));
    }

    /**
     * Returns the first email contact with classification 'REMINDER' or the
     * preferred email contact if no contact has this classification.
     *
     * @param contacts the contacts
     * @return a contact, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    public Contact getEmailContact(Set<Contact> contacts) {
        return getContact(contacts, false, ContactArchetypes.EMAIL);
    }

    /**
     * Returns the reminder types associated with a product.
     *
     * @param product the product
     * @return the associated reminder types, keyed on their <em>entityRelationship.productReminder</em>
     */
    public Map<EntityRelationship, Entity> getReminderTypes(Product product) {
        Map<EntityRelationship, Entity> result;
        IMObjectBean bean = new IMObjectBean(product, service);
        if (bean.hasNode("reminders")) {
            result = bean.getNodeTargetObjects("reminders", Entity.class, EntityRelationship.class);
        } else {
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * Returns a reminder associated with an <em>act.patientDocumentForm</em>.
     * <p/>
     * For forms linked to an invoice item (via <em>actRelationship.invoiceItemDocument)</em>, this
     * uses the invoice item to get the reminder. If there are multiple reminders for the invoice item,
     * the one with the nearest due date will be returned.
     * <br/>
     * If there are multiple reminders with the same due date, the reminder with the lesser id will be used.
     * <p/>
     * For forms not linked to an invoice item that have a product with reminders, a reminder with the nearest due date
     * to that of the form's start time will be returned.
     * <p/>
     * For forms that don't meet the above, {@code null} is returned.
     *
     * @param form the form
     * @return the reminder, or {@code null} if there are no associated reminders
     */
    public Act getDocumentFormReminder(DocumentAct form) {
        Act result;
        ActBean formBean = new ActBean(form, service);
        Act invoiceItem = formBean.getSourceAct("actRelationship.invoiceItemDocument");
        if (invoiceItem != null) {
            result = getInvoiceReminder(invoiceItem);
        } else {
            result = getProductReminder(formBean);
        }
        return result;
    }

    /**
     * Determines the due state of a reminder relative to the current date.
     *
     * @param reminder the reminder
     * @return the due state
     */
    public DueState getDueState(Act reminder) {
        return getDueState(reminder, new Date());
    }

    /**
     * Determines the due state of a reminder relative to the specified date.
     *
     * @param reminder the reminder
     * @param date     the date
     * @return the due state
     */
    public DueState getDueState(Act reminder, Date date) {
        ActBean act = new ActBean(reminder, service);
        DueState result = DueState.NOT_DUE;
        Entity reminderType = act.getParticipant(ReminderArchetypes.REMINDER_TYPE_PARTICIPATION);
        if (reminderType != null) {
            EntityBean bean = new EntityBean(reminderType);
            String sensitivityUnits = bean.getString("sensitivityUnits");
            if (sensitivityUnits == null) {
                sensitivityUnits = DateUnits.DAYS.toString();
            }
            int interval = bean.getInt("sensitivityInterval");
            DateUnits units = DateUnits.valueOf(sensitivityUnits);
            Date from = DateRules.getDate(date, -interval, units);
            Date to = DateRules.getDate(date, interval, units);
            Date dueDate = act.getAct().getActivityEndTime();
            if (dueDate != null) {
                if (DateRules.compareTo(dueDate, from) < 0) {
                    result = DueState.OVERDUE;
                } else if (DateRules.compareTo(dueDate, to) <= 0) {
                    result = DueState.DUE;
                }
            }
        }
        return result;
    }

    /**
     * Returns a reminder associated with an invoice item.
     * <p/>
     * If there are multiple reminders for the invoice item, the one with the nearest due date will be returned.
     *
     * @param invoiceItem the invoice item
     * @return the reminder, or {@code null} if there are no associated reminders
     */
    private Act getInvoiceReminder(Act invoiceItem) {
        Act result = null;
        Date resultDueDate = null;
        ActBean bean = new ActBean(invoiceItem, service);
        List<Act> reminders = bean.getNodeActs("reminders");
        for (Act reminder : reminders) {
            Date dueDate = reminder.getActivityEndTime();
            if (dueDate != null) {
                boolean found = false;
                if (result == null) {
                    found = true;
                } else {
                    int compare = DateRules.compareTo(dueDate, resultDueDate);
                    if (compare < 0 || (compare == 0 && reminder.getId() < result.getId())) {
                        found = true;
                    }
                }
                if (found) {
                    result = reminder;
                    resultDueDate = dueDate;
                }
            }
        }
        return result;
    }

    /**
     * Returns a product reminder with the nearest due date to that of the forms start time will be returned.
     *
     * @param formBean the <em>act.patientDocumentForm</em> bean
     * @return the reminder, or {@code null} if there are no reminders associated with the product
     */
    private Act getProductReminder(ActBean formBean) {
        Act result = null;
        Date resultDueDate = null;
        Product product = (Product) formBean.getNodeParticipant("product");
        if (product != null) {
            Party patient = (Party) formBean.getNodeParticipant("patient");
            Date startTime = formBean.getDate("startTime");
            Map<EntityRelationship, Entity> reminderTypes = getReminderTypes(product);
            for (Map.Entry<EntityRelationship, Entity> entry : reminderTypes.entrySet()) {
                EntityRelationship relationship = entry.getKey();
                Entity reminderType = entry.getValue();
                Date dueDate = calculateProductReminderDueDate(startTime, relationship);
                if (resultDueDate == null || DateRules.compareTo(dueDate, resultDueDate) < 1) {
                    result = createReminder(reminderType, startTime, dueDate, patient, product);
                    resultDueDate = dueDate;
                }
            }
        }
        return result;
    }

    /**
     * Determines if a reminder is associated with an <em>entity.reminderType</em> that is the same as that specified
     * or has one or more <em>lookup.reminderGroup</em> classifications the same as those specified.
     *
     * @param reminder     the reminder
     * @param reminderType the reminder type
     * @return {@code true} if the reminder has a matching type or group
     * @throws ArchetypeServiceException for any archetype service error
     */
    protected boolean hasMatchingTypeOrGroup(Act reminder, ReminderType reminderType) {
        boolean result = false;
        ReminderType otherType = getReminderType(reminder);
        if (otherType != null) {
            if (otherType.getEntity().equals(reminderType.getEntity())) {
                result = true;
            } else {
                List<Lookup> groups = reminderType.getGroups();
                for (Lookup group : otherType.getGroups()) {
                    if (groups.contains(group)) {
                        result = true;
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the reminder type associated with an act.
     *
     * @param act the act
     * @return the associated reminder type, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    protected ReminderType getReminderType(Act act) {
        return getReminderType(new ActBean(act, service));
    }

    /**
     * Returns the reminder type associated with an act.
     *
     * @param bean the act bean
     * @return the associated reminder type, or {@code null} if none is found
     * @throws ArchetypeServiceException for any archetype service error
     */
    protected ReminderType getReminderType(ActBean bean) {
        ReminderType reminderType = null;
        if (reminderTypes != null) {
            reminderType = reminderTypes.get(
                    bean.getParticipantRef(ReminderArchetypes.REMINDER_TYPE_PARTICIPATION));
        } else {
            Entity entity = bean.getParticipant(ReminderArchetypes.REMINDER_TYPE_PARTICIPATION);
            if (entity != null) {
                reminderType = new ReminderType(entity, service);
            }
        }
        return reminderType;
    }

    /**
     * Sets a reminder's status to completed, and updates its completedDate
     * to 'now' before saving it.
     *
     * @param reminder the reminder
     * @throws ArchetypeServiceException for any archetype service error
     */
    protected void markCompleted(Act reminder) {
        ActBean bean = new ActBean(reminder, service);
        bean.setStatus(ReminderStatus.COMPLETED);
        bean.setValue("completedDate", new Date());
        bean.save();
    }

    /**
     * Sets any IN_PROGRESS reminders that have the same patient and matching reminder group and/or type as that in
     * the supplied reminder to COMPLETED.
     * <p/>
     * This only has effect if the reminder has IN_PROGRESS status.
     * <p/>
     * If the reminder is set to expire, it is also marked COMPLETED.
     *
     * @param reminder the reminder
     * @throws ArchetypeServiceException for any archetype service exception
     */
    private void doMarkMatchingRemindersCompleted(Act reminder) {
        if (ReminderStatus.IN_PROGRESS.equals(reminder.getStatus())) {
            ActBean bean = new ActBean(reminder, service);
            ReminderType reminderType = getReminderType(bean);
            IMObjectReference patient = bean.getNodeParticipantRef("patient");
            if (reminderType != null && patient != null) {
                doMarkMatchingRemindersCompleted(reminder, reminderType, patient);
            }
        }
    }

    /**
     * Sets any IN_PROGRESS reminders that have the same patient and matching reminder group and/or type as that in
     * the supplied reminder to COMPLETED.
     * <p/>
     * This only has effect if the reminder has IN_PROGRESS status.
     * <p/>
     * If the reminder is set to expire, it is also marked COMPLETED.
     *
     * @param reminder     the reminder
     * @param reminderType the reminder type
     * @param patient      the patient reference
     * @throws ArchetypeServiceException for any archetype service exception
     */
    private void doMarkMatchingRemindersCompleted(Act reminder, ReminderType reminderType, IMObjectReference patient) {
        ArchetypeQuery query = new ArchetypeQuery(ReminderArchetypes.REMINDER, false, true);
        query.add(Constraints.eq("status", ReminderStatus.IN_PROGRESS));
        query.add(Constraints.join("patient").add(Constraints.eq("entity", patient)));
        if (!reminder.isNew()) {
            query.add(Constraints.ne("id", reminder.getId()));
        }
        query.setMaxResults(ArchetypeQuery.ALL_RESULTS); // must query all, otherwise the iteration would change
        IMObjectQueryIterator<Act> reminders = new IMObjectQueryIterator<Act>(service, query);
        while (reminders.hasNext()) {
            Act act = reminders.next();
            if (hasMatchingTypeOrGroup(act, reminderType)) {
                markCompleted(act);
            }
        }
        // if the reminder is set to expire immediately, mark it COMPLETED
        if (reminderType.shouldCancel(reminder.getActivityEndTime(), new Date())) {
            markCompleted(reminder);
        }
    }

    /**
     * Helper to return a count from a named query.
     *
     * @param query the query
     * @return the count
     * @throws ArchetypeServiceException for any error
     */
    private int count(NamedQuery query) {
        Iterator<ObjectSet> iter = new ObjectSetQueryIterator(service, query);
        if (iter.hasNext()) {
            ObjectSet set = iter.next();
            Number count = (Number) set.get("count");
            return count != null ? count.intValue() : 0;
        }
        return 0;
    }

    /**
     * Returns the first contact with classification 'REMINDER' or the preferred contact with the specified short name
     * if no contact has this classification.
     *
     * @param contacts   the contacts
     * @param anyContact if {@code true} any contact with a 'REMINDER classification will be returned.
     * @param shortName  the archetype shortname of the preferred contact
     * @return a contact, or {@code null} if none is found
     */
    private Contact getContact(Set<Contact> contacts, boolean anyContact, String shortName) {
        List<Contact> list = Contacts.sort(contacts); // sort to make it deterministic
        Contact result = Contacts.find(list, new PurposeMatcher(shortName, REMINDER, service));
        if (result == null && anyContact) {
            result = Contacts.find(list, new PurposeMatcher("contact.*", REMINDER, service));
            if (result == null) {
                // no contact found with reminder purpose, so use the preferred contact with the specified short name.
                result = Contacts.find(list, new PurposeMatcher(shortName, null, false, service));
                if (result == null) {
                    // no contact found with the short name, so use the first available preferred contact, or
                    // if none preferred, the first available
                    result = Contacts.find(list, new PurposeMatcher("contact.*", null, false, service));
                }
            }
        }
        return result;
    }

    /**
     * Creates a reminder.
     *
     * @param reminderType the reminder type
     * @param startTime    the reminder start time
     * @param dueDate      the reminder due date
     * @param patient      the patient
     * @param product      the product. May be {@code null}
     * @return a new reminder
     * @throws ArchetypeServiceException for any error
     */
    private Act createReminder(Entity reminderType, Date startTime, Date dueDate, Party patient, Product product) {
        Act result = (Act) service.create("act.patientReminder");
        ActBean bean = new ActBean(result, service);
        bean.addNodeParticipation("reminderType", reminderType);
        bean.addNodeParticipation("patient", patient);
        if (product != null) {
            bean.addNodeParticipation("product", product);
        }
        result.setActivityStartTime(startTime);
        result.setActivityEndTime(dueDate);
        return result;
    }

}
