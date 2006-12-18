/*
 *  Version: 1.0
 *
 *  The contents of this file are subject to the OpenVPMS License Version
 *  1.0 (the 'License'); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *  http://www.openvpms.org/license/
 *
 *  Software distributed under the License is distributed on an 'AS IS' basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 *  for the specific language governing rights and limitations under the
 *  License.
 *
 *  Copyright 2006 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.test;

import junit.framework.Assert;
import org.openvpms.archetype.rules.patient.PatientRules;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.party.Contact;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.ValidationException;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;


/**
 * Unit test helper.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class TestHelper extends Assert {

    /**
     * Creates a new object.
     *
     * @param shortName the archetype short name
     * @return a new object
     */
    public static IMObject create(String shortName) {
        IArchetypeService service = ArchetypeServiceHelper.getArchetypeService();
        IMObject object = service.create(shortName);
        assertNotNull(object);
        return object;
    }

    /**
     * Helper to save an object.
     *
     * @param object the object to save
     * @throws ArchetypeServiceException if the service cannot save the object
     * @throws ValidationException       if the object cannot be validated
     */
    public static void save(IMObject object) {
        ArchetypeServiceHelper.getArchetypeService().save(object);
    }

    /**
     * Creates a new customer.
     *
     * @param firstName the customer's first name
     * @param lastName  the customer's surname
     * @param save      if <code>true</code> make the customer persistent
     * @return a new customer
     */
    public static Party createCustomer(String firstName, String lastName,
                                       boolean save) {
        Party customer = (Party) create("party.customerperson");
        IMObjectBean bean = new IMObjectBean(customer);
        bean.setValue("firstName", firstName);
        bean.setValue("lastName", lastName);
        Contact contact = (Contact) create("contact.phoneNumber");
        assertNotNull(contact);
        customer.addContact(contact);
        if (save) {
            bean.save();
        }
        return customer;
    }

    /**
     * Creates and saves a new customer.
     *
     * @return a new customer
     */
    public static Party createCustomer() {
        return createCustomer(true);
    }

    /**
     * Creates a new customer.
     *
     * @param save if <code>true</code> make the customer persistent
     * @return a new customer
     */
    public static Party createCustomer(boolean save) {

        return createCustomer("J", "Zoo-" + System.currentTimeMillis(), save);
    }

    /**
     * Creates and saves a new patient.
     *
     * @return a new patient
     */
    public static Party createPatient() {
        return createPatient(true);
    }

    /**
     * Creates a new patient.
     *
     * @param save if <code>true</code> make the patient persistent
     * @return a new patient
     */
    public static Party createPatient(boolean save) {
        Party patient = (Party) create("party.patientpet");
        assertNotNull(patient);
        EntityBean bean = new EntityBean(patient);
        bean.setValue("name", "XPatient-" + System.currentTimeMillis());
        bean.setValue("species", "Canine");
        if (save) {
            bean.save();
        }
        return patient;
    }

    /**
     * Creates and saves a new patient, owned by the specified customer.
     *
     * @param owner the patient owner
     * @return a new patient
     */
    public static Party createPatient(Party owner) {
        return createPatient(owner, true);
    }

    /**
     * Creates a new patient, owned by the specified customer.
     *
     * @param owner the patient owner
     * @param save  if <code>true</code>, make the patient persistent
     * @return a new patient
     */
    public static Party createPatient(Party owner, boolean save) {
        Party patient = createPatient(save);
        PatientRules rules = new PatientRules(
                ArchetypeServiceHelper.getArchetypeService());
        rules.addPatientOwnerRelationship(owner, patient);
        if (save) {
            save(patient);
        }
        return patient;
    }

    /**
     * Creates and saves a new clinician.
     *
     * @return a new clinician
     */
    public static User createClinician() {
        return createClinician(true);
    }

    /**
     * Creates a new clinician.
     *
     * @param save if <code>true</code> make the user persistent
     * @return a new user
     */
    public static User createClinician(boolean save) {
        User user = (User) create("security.user");
        assertNotNull(user);
        EntityBean bean = new EntityBean(user);
        bean.setValue("name", "zvet" + System.currentTimeMillis());
        bean.setValue("username", "zvet");
        bean.setValue("password", "zvet");
        if (save) {
            bean.save();
        }
        return user;
    }

}