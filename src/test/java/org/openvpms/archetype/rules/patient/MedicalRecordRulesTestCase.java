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
 * Copyright 2013 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.rules.patient;

import org.junit.Before;
import org.junit.Test;
import org.openvpms.archetype.rules.act.ActStatus;
import org.openvpms.archetype.rules.finance.account.CustomerAccountArchetypes;
import org.openvpms.archetype.rules.finance.account.FinancialTestHelper;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.datatypes.quantity.Money;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.helper.ActBean;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link MedicalRecordRules} class.
 * Note: this requires the archetype service to be configured to trigger the
 * <em>archetypeService.remove.act.patientClinicalEvent.after.drl</em> rule.
 *
 * @author Tim Anderson
 */
public class MedicalRecordRulesTestCase extends ArchetypeServiceTest {

    /**
     * The patient.
     */
    private Party patient;

    /**
     * The clinician.
     */
    private User clinician;

    /**
     * The rules.
     */
    private MedicalRecordRules rules;


    /**
     * Verifies that deletion of an <em>act.patientClinicalEvent</em>
     * deletes all of the children, except invoice items.
     */
    @Test
    public void testDeleteClinicalEvent() {
        Act event = createEvent();
        Act problem = createProblem();
        Act item = createInvoiceItem();
        Act medication1 = createMedication(patient);
        Act medication2 = createMedication(patient);

        // link each of the acts to the event
        ActBean eventBean = new ActBean(event);
        eventBean.addNodeRelationship("items", problem);
        eventBean.addNodeRelationship("items", medication1);
        eventBean.addNodeRelationship("items", medication2);
        eventBean.addNodeRelationship("chargeItems", item);

        // link medication2 to the invoice item
        ActBean itemBean = new ActBean(item);
        itemBean.addNodeRelationship("dispensing", medication2);

        // save them all
        save(event, problem, item, medication1, medication2);


        // make sure each of the objects can be retrieved
        event = get(event);
        assertNotNull(event);
        eventBean = new ActBean(event);
        assertTrue(eventBean.hasNodeTarget("items", problem));
        assertTrue(eventBean.hasNodeTarget("items", medication1));
        assertTrue(eventBean.hasNodeTarget("items", medication2));
        assertTrue(eventBean.hasNodeTarget("chargeItems", item));

        assertNotNull(get(problem.getObjectReference()));
        assertNotNull(get(item.getObjectReference()));
        assertNotNull(get(medication1.getObjectReference()));
        assertNotNull(get(medication2.getObjectReference()));

        // now remove the event
        remove(event);

        // make sure the event, problem and medication1, are deleted, but the invoice item and medication2 remains
        assertNull(get(event.getObjectReference()));
        assertNull(get(problem.getObjectReference()));
        assertNotNull(get(item.getObjectReference()));
        assertNotNull(get(medication2.getObjectReference()));
    }

    /**
     * Verifies that deletion of an <em>act.patientClinicalProblem</em>
     * doesn't affect its children.
     */
    @Test
    public void testDeleteClinicalProblem() {
        Act event = createEvent();
        Act problem = createProblem();
        Act note = createNote();
        ActBean eventBean = new ActBean(event);
        eventBean.addRelationship("actRelationship.patientClinicalEventItem",
                                  problem);
        eventBean.addRelationship("actRelationship.patientClinicalEventItem",
                                  note);
        ActBean problemBean = new ActBean(problem);
        problemBean.addRelationship(
                "actRelationship.patientClinicalProblemItem",
                note);
        save(event, problem, note);

        // make sure each of the objects can be retrieved
        assertNotNull(get(event.getObjectReference()));
        assertNotNull(get(problem.getObjectReference()));

        remove(problem);  // remove shouldn't cascade to delete note

        // make sure the all but the problem can be retrieved
        assertNotNull(get(event));
        assertNull(get(problem));
        assertNotNull(get(note));
    }

    /**
     * Tests the {@link MedicalRecordRules#getEvent(IMObjectReference)}
     * method.
     */
    @Test
    public void testGetEvent() {
        Act event1 = createEvent(getDate("2007-01-01"));
        event1.setStatus(ActStatus.IN_PROGRESS);
        save(event1);
        checkEvent(event1);

        Act event2 = createEvent(getDate("2007-01-02"));
        event2.setStatus(ActStatus.COMPLETED);
        save(event2);
        checkEvent(event2);

        Act event3 = createEvent(getDate("2008-01-01"));
        event3.setStatus(ActStatus.IN_PROGRESS);
        save(event3);
        checkEvent(event3);

        // ensure that where there are 2 events with the same timestamp, the one with the higher id is returned
        Act event4 = createEvent(getDate("2008-01-01"));
        event4.setStatus(ActStatus.IN_PROGRESS);
        save(event4);
        checkEvent(event4);
    }

    /**
     * Tests the {@link MedicalRecordRules#getEvent} method.
     */
    @Test
    public void testGetEventByDate() {
        Date jan1 = getDate("2007-01-01");
        Date jan2 = getDate("2007-01-02");
        Date jan3 = getDate("2007-01-03 10:43:55");

        checkEvent(jan2, null);

        Act event1 = createEvent(jan2);
        save(event1);

        checkEvent(jan2, event1);
        checkEvent(jan1, null);
        checkEvent(jan3, event1);

        event1.setActivityEndTime(jan2);
        save(event1);
        checkEvent(jan1, null);
        checkEvent(jan3, null);

        Act event2 = createEvent(jan3);
        save(event2);
        checkEvent(jan3, event2);
        checkEvent(getDate("2007-01-03"), event2);
        // note that the time component is zero, but still picks up event2,
        // despite the event being created after 00:00:00. This is required
        // as the time component of startTime is not supplied consistently -
        // In some cases, it is present, in others it is 00:00:00.

        checkEvent(jan2, event1);

        // make sure that when an event has a duplicate timestamp, the earliest (by id) is returned
        Act event2dup = createEvent(jan3);
        save(event2dup);
        checkEvent(jan3, event2);
    }

    /**
     * Tests the {@link MedicalRecordRules#addNote} method.
     */
    @Test
    public void testAddNote() {
        Act event = createEvent();
        Date startTime = getDate("2012-07-17");
        User author = TestHelper.createUser();
        User clinician = TestHelper.createClinician();
        String text = "Test note";
        Act note = rules.addNote(event, startTime, text, clinician, author);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note));
        ActBean bean = new ActBean(note);
        assertEquals(startTime, note.getActivityStartTime());
        assertEquals(text, bean.getString("note"));
        assertEquals(patient, bean.getNodeParticipant("patient"));
        assertEquals(clinician, bean.getNodeParticipant("clinician"));
        assertEquals(author, bean.getNodeParticipant("author"));
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where no event
     * exists for the patient. A new one will be created with COMPLETED status,
     * and the specified startTime.
     */
    @Test
    public void testAddToEventForNonExistentEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        assertTrue(ActStatus.COMPLETED.equals(event.getStatus()));
        assertEquals(date, event.getActivityStartTime());
        checkContains(event, medication);
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where there is
     * an existing IN_PROGRESS event that has a startTime < 7 days prior to
     * that specified. The medication should be added to it.
     */
    @Test
    public void testAddToEventForExistingInProgressEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        Act expected = createEvent(date);
        save(expected);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        checkContains(event, medication);
        assertEquals(expected, event);
        assertTrue(ActStatus.IN_PROGRESS.equals(event.getStatus()));
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where
     * there is an IN_PROGRESS event that has a startTime > 7 days prior to
     * the specified startTime. A new COMPLETED event should be created.
     */
    @Test
    public void testAddToEventForExistingOldInProgressEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        Date old = DateRules.getDate(date, -8, DateUnits.DAYS);
        Act oldEvent = createEvent(old);
        save(oldEvent);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        checkContains(event, medication);
        assertFalse(oldEvent.equals(event));
        assertEquals(date, event.getActivityStartTime());
        assertTrue(ActStatus.COMPLETED.equals(event.getStatus()));
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where
     * there is a COMPLETED event that has a startTime > 7 days prior to
     * the specified startTime. A new COMPLETED event should be created.
     */
    public void testAddToEventForExistingOldCompletedEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        Date old = DateRules.getDate(date, -8, DateUnits.DAYS);
        Act oldEvent = createEvent(old);
        oldEvent.setStatus(ActStatus.COMPLETED);
        save(oldEvent);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        checkContains(event, medication);
        assertFalse(oldEvent.equals(event));
        assertEquals(date, event.getActivityStartTime());
        assertTrue(ActStatus.COMPLETED.equals(event.getStatus()));
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where
     * there is a COMPLETTED event that has a startTime and endTime that
     * overlaps the specified start time. The medication should be added to it.
     */
    @Test
    public void testAddToEventForExistingCompletedEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        Act completed = createEvent(getDate("2007-04-03"));
        completed.setActivityEndTime(getDate("2007-04-06"));
        completed.setStatus(ActStatus.COMPLETED);
        save(completed);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        checkContains(event, medication);
        assertEquals(completed, event);
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvent} method where
     * there is a COMPLETTED event that has a startTime and endTime that
     * DOESN'T overlap the specified start time. The medication should be added
     * to a new COMPLETED event whose startTime equals that specified.
     */
    @Test
    public void testAddToEventForExistingNonOverlappingCompletedEvent() {
        Date date = getDate("2007-04-05");
        Act medication = createMedication(patient);
        save(medication);

        Act completed = createEvent(getDate("2007-04-03"));
        completed.setActivityEndTime(getDate("2007-04-04"));
        completed.setStatus(ActStatus.COMPLETED);
        save(completed);

        rules.addToEvent(medication, date);
        Act event = rules.getEvent(patient);
        checkContains(event, medication);
        assertFalse(completed.equals(event));
        assertEquals(date, event.getActivityStartTime());
        assertTrue(ActStatus.COMPLETED.equals(event.getStatus()));
    }

    /**
     * Tests the {@link org.openvpms.archetype.rules.patient.MedicalRecordRules#linkMedicalRecords} method.
     */
    @Test
    public void testLinkMedicalRecords() {
        Act event = createEvent();
        Act problem = createProblem();
        Act note = createNote();
        rules.linkMedicalRecords(event, problem, note);

        event = get(event);
        problem = get(problem);
        note = get(note);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, problem));
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note));

        ActBean problemBean = new ActBean(problem);
        assertTrue(problemBean.hasRelationship(PatientArchetypes.CLINICAL_PROBLEM_ITEM, note));

        // verify that it can be called again with no ill effect
        rules.linkMedicalRecords(event, problem, note);
    }

    /**
     * Tests the {@link MedicalRecordRules#linkMedicalRecords(Act, Act)} method
     * passing an <em>act.patientClinicalNote</em>.
     */
    @Test
    public void testLinkMedicalRecordsWithItem() {
        Act event = createEvent();
        Act note = createNote();

        rules.linkMedicalRecords(event, note);

        event = get(event);
        note = get(note);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note));

        // verify that it can be called again with no ill effect
        rules.linkMedicalRecords(event, note);
    }

    /**
     * Tests the {@link MedicalRecordRules#linkMedicalRecords(Act, Act)} method passing
     * an <em>act.customerAccountInvoiceItem</em>.
     */
    @Test
    public void testLinkMedicalRecordsWithInvoiceItem() {
        Act event = createEvent();
        Act invoiceItem = FinancialTestHelper.createItem(CustomerAccountArchetypes.INVOICE_ITEM,
                                                         Money.ONE, patient, TestHelper.createProduct());
        save(invoiceItem);
        rules.linkMedicalRecords(event, invoiceItem);

        event = get(event);
        invoiceItem = get(invoiceItem);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_CHARGE_ITEM, invoiceItem));

        // verify that it can be called again with no ill effect
        rules.linkMedicalRecords(event, invoiceItem);
    }

    /**
     * Tests the {@link MedicalRecordRules#linkMedicalRecords(Act, Act)} method,
     * passing an <em>act.patientClinicalProblem</em>.
     */
    @Test
    public void testLinkMedicalRecordsWithProblem() {
        Act event = createEvent();
        Act problem = createProblem();
        Act note = createNote();

        ActBean problemBean = new ActBean(problem);
        problemBean.addNodeRelationship("items", note);
        save(problem, note);

        rules.linkMedicalRecords(event, problem);

        event = get(event);
        problem = get(problem);
        note = get(note);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note));
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, problem));

        // verify that it can be called again with no ill effect
        rules.linkMedicalRecords(event, problem);
    }

    /**
     * Verifies the {@link MedicalRecordRules#linkMedicalRecords(Act, Act, Act)} method,
     * links all of a problem's items to the parent event if they aren't already present.
     */
    @Test
    public void testLinkMedicalRecordsForMissingLinks() {
        Act event = createEvent();
        Act problem = createProblem();
        Act note1 = createNote();
        Act note2 = createNote();
        Act medication = createMedication(patient);
        ActBean problemBean = new ActBean(problem);
        problemBean.addNodeRelationship("items", note1);
        problemBean.addNodeRelationship("items", medication);
        save(problem, note1, medication);

        // now link the records to the event
        rules.linkMedicalRecords(event, problem, note2);

        event = get(event);
        problem = get(problem);
        note1 = get(note1);
        note2 = get(note2);
        medication = get(medication);

        ActBean eventBean = new ActBean(event);
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, problem));
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note1));
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, note2));
        assertTrue(eventBean.hasRelationship(PatientArchetypes.CLINICAL_EVENT_ITEM, medication));

        problemBean = new ActBean(problem);
        assertTrue(problemBean.hasRelationship(PatientArchetypes.CLINICAL_PROBLEM_ITEM, note1));
        assertTrue(problemBean.hasRelationship(PatientArchetypes.CLINICAL_PROBLEM_ITEM, note2));
        assertTrue(problemBean.hasRelationship(PatientArchetypes.CLINICAL_PROBLEM_ITEM, medication));
    }

    /**
     * Tests the {@link MedicalRecordRules#addToEvents} method.
     */
    @Test
    public void testAddToEvents() {
        Date date = getDate("2007-04-05");
        Party patient2 = TestHelper.createPatient();
        Act med1 = createMedication(patient);
        Act med2 = createMedication(patient);
        Act med3 = createMedication(patient2);
        Act med4 = createMedication(patient2);

        save(med1);
        save(med2);
        save(med3);
        save(med4);

        List<Act> acts = Arrays.asList(med1, med2, med3, med4);

        Act event1 = createEvent(date);
        save(event1);
        rules.addToEvents(acts, date);

        event1 = rules.getEvent(patient, date);
        checkContains(event1, med1, med2);

        Act event2 = rules.getEvent(patient2, date);
        assertNotNull(event2);
        checkContains(event2, med3, med4);
    }

    /**
     * Tests the {@link MedicalRecordRules#addToHistoricalEvents} method.
     */
    @Test
    public void testAddToHistoricalEvents() {
        Date eventDate1 = getDate("2007-04-05");
        Date eventDate2 = getDate("2007-07-01");
        Date eventDate3 = getDate("2007-08-01");
        Act med1 = createMedication(patient);
        Act med2 = createMedication(patient);
        Act med3 = createMedication(patient);

        Date medDate1 = getDate("2007-04-04"); // eventDate1-1
        med1.setActivityStartTime(medDate1);
        save(med1);

        med2.setActivityStartTime(eventDate2);
        save(med2);

        med3.setActivityStartTime(eventDate3);
        save(med3);

        Act event1 = createEvent(eventDate1);
        save(event1);

        Act event2 = createEvent(eventDate2);
        save(event2);

        Act event3 = createEvent(eventDate3);
        save(event3);

        rules.addToHistoricalEvents(Arrays.asList(med1), eventDate1);
        rules.addToHistoricalEvents(Arrays.asList(med2), eventDate2);
        rules.addToHistoricalEvents(Arrays.asList(med3), eventDate3);

        event1 = rules.getEvent(patient, eventDate1);
        checkContains(event1, med1);

        event2 = rules.getEvent(patient, eventDate2);
        assertNotNull(event2);
        checkContains(event2, med2);

        event3 = rules.getEvent(patient, eventDate3);
        assertNotNull(event3);
        checkContains(event3, med3);
    }

    /**
     * Sets up the test case.
     */
    @Before
    public void setUp() {
        clinician = TestHelper.createClinician();
        patient = TestHelper.createPatient();
        rules = new MedicalRecordRules(getArchetypeService());
    }

    /**
     * Helper to create an <em>act.patientClinicalEvent</em>.
     *
     * @return a new act
     */
    protected Act createEvent() {
        Act act = createAct("act.patientClinicalEvent");
        ActBean bean = new ActBean(act);
        bean.addParticipation("participation.patient", patient);
        bean.addParticipation("participation.clinician", clinician);
        return act;
    }

    /**
     * Helper to create an <em>act.patientClinicalEvent</em>.
     *
     * @param startTime the start time
     * @return a new act
     */
    protected Act createEvent(Date startTime) {
        Act act = createEvent();
        act.setActivityStartTime(startTime);
        return act;
    }

    /**
     * Helper to create an <em>act.patientClinicalProblem</em>.
     *
     * @return a new act
     */
    protected Act createProblem() {
        Act act = createAct("act.patientClinicalProblem");
        ActBean bean = new ActBean(act);
        bean.addParticipation("participation.patient", patient);
        bean.addParticipation("participation.clinician", clinician);
        return act;
    }

    /**
     * Helper to create an <em>act.patientClinicalNote</em>.
     *
     * @return a new act
     */
    protected Act createNote() {
        Act act = createAct("act.patientClinicalNote");
        ActBean bean = new ActBean(act);
        bean.addParticipation("participation.patient", patient);
        return act;
    }

    /**
     * Helper to create an <em>act.patientMedication</em>.
     *
     * @param patient the patient
     * @return a new act
     */
    protected Act createMedication(Party patient) {
        Act act = createAct("act.patientMedication");
        ActBean bean = new ActBean(act);
        bean.addParticipation("participation.patient", patient);
        Product product = TestHelper.createProduct();
        bean.addParticipation("participation.product", product);
        return act;
    }

    /**
     * Helper to create a new act.
     *
     * @param shortName the act short name
     * @return a new act
     */
    protected Act createAct(String shortName) {
        Act act = (Act) create(shortName);
        assertNotNull(act);
        return act;
    }


    /**
     * Helper to create an <em>act.customerAccountInvoiceItem</em>.
     *
     * @return a new act
     */
    private Act createInvoiceItem() {
        return FinancialTestHelper.createItem(CustomerAccountArchetypes.INVOICE_ITEM, Money.ONE, patient,
                                              TestHelper.createProduct());
    }

    /**
     * Verifies that the correct event is returned.
     *
     * @param expected the expected event. May be <tt>null</tt>
     */
    private void checkEvent(Act expected) {
        Act event = rules.getEvent(patient);
        if (expected == null) {
            assertNull(event);
        } else {
            assertEquals(expected, event);
        }
    }

    /**
     * Verifies that the correct event is returned for a particular date.
     *
     * @param date     the date
     * @param expected the expected event. May be <tt>null</tt>
     */
    private void checkEvent(Date date, Act expected) {
        Act event = rules.getEvent(patient, date);
        if (expected == null) {
            assertNull(event);
        } else {
            assertEquals(expected, event);
        }
    }

    /**
     * Verifies that an event contains a set of acts.
     *
     * @param event the event
     * @param acts  the expected acts
     */
    private void checkContains(Act event, Act... acts) {
        List<Act> items = getActs(event);
        assertEquals(acts.length, items.size());
        for (Act act : acts) {
            boolean found = false;
            for (Act item : items) {
                if (item.equals(act)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }
    }

    /**
     * Returns the items linked to an event.
     *
     * @param event the event
     * @return the items
     */
    private List<Act> getActs(Act event) {
        ActBean bean = new ActBean(event);
        return bean.getNodeActs("items");
    }

    /**
     * Returns a date for a date string.
     *
     * @param date the stringified date
     * @return the date
     */
    private Date getDate(String date) {
        if (date.contains(":")) {
            return Timestamp.valueOf(date);
        } else {
            return java.sql.Date.valueOf(date);
        }
    }

}
