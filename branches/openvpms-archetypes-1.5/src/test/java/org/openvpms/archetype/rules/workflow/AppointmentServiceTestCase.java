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
 *  Copyright 2008 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.rules.workflow;

import org.junit.Before;
import org.junit.Test;
import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.system.common.util.PropertySet;

import java.util.Date;
import java.util.List;


/**
 * Tests the {@link AppointmentService}.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class AppointmentServiceTestCase extends ArchetypeServiceTest {

    /**
     * The appointment service.
     */
    private ScheduleService service;

    /**
     * The schedule.
     */
    private Party schedule;


    /**
     * Tests addition of an appointment.
     */
    @Test
    public void testAddEvent() {
        Date date1 = java.sql.Date.valueOf("2008-1-1");
        Date date2 = java.sql.Date.valueOf("2008-1-2");
        Date date3 = java.sql.Date.valueOf("2008-1-3");

        // retrieve the appointments for date1 and date2 and verify they are
        // empty.
        // This caches the appointments for each date.
        List<PropertySet> results = service.getEvents(schedule, date1);
        assertEquals(0, results.size());

        results = service.getEvents(schedule, date2);
        assertEquals(0, results.size());

        // create and save appointment for date1
        Act appointment = createAppointment(date1);

        results = service.getEvents(schedule, date1);
        assertEquals(1, results.size());
        PropertySet set = results.get(0);
        checkAppointment(appointment, set);

        results = service.getEvents(schedule, date2);
        assertEquals(0, results.size());

        results = service.getEvents(schedule, date3);
        assertEquals(0, results.size());
    }

    /**
     * Tests removal of an event.
     */
    @Test
    public void testRemoveEvent() {
        Date date1 = java.sql.Date.valueOf("2008-1-1");
        Date date2 = java.sql.Date.valueOf("2008-1-2");
        Date date3 = java.sql.Date.valueOf("2008-1-3");

        // retrieve the appointments for date1 and date2 and verify they are
        // empty.
        // This caches the appointments for each date.
        List<PropertySet> results = service.getEvents(schedule, date1);
        assertEquals(0, results.size());

        results = service.getEvents(schedule, date2);
        assertEquals(0, results.size());

        // create and save appointment for date1
        Act appointment = createAppointment(date1);

        results = service.getEvents(schedule, date1);
        assertEquals(1, results.size());

        results = service.getEvents(schedule, date2);
        assertEquals(0, results.size());

        // now remove it
        getArchetypeService().remove(appointment);

        // verify it has been removed
        assertEquals(0, service.getEvents(schedule, date1).size());
        assertEquals(0, service.getEvents(schedule, date2).size());
        assertEquals(0, service.getEvents(schedule, date3).size());
    }

    /**
     * Tests the {@link AppointmentService#getEvents(Entity, Date)} ()} method.
     */
    @Test
    public void testGetEvents() {
        final int count = 10;
        Party schedule = ScheduleTestHelper.createSchedule();
        Act[] appointments = new Act[count];
        Date date = java.sql.Date.valueOf("2007-1-1");
        for (int i = 0; i < count; ++i) {
            Date startTime = DateRules.getDate(date, 15 * count,
                                               DateUnits.MINUTES);
            Date endTime = DateRules.getDate(startTime, 15, DateUnits.MINUTES);
            Date arrivalTime = (i % 2 == 0) ? new Date() : null;
            Party customer = TestHelper.createCustomer();
            Party patient = TestHelper.createPatient();
            User clinician = TestHelper.createClinician();
            Act appointment = ScheduleTestHelper.createAppointment(
                    startTime, endTime, schedule, customer, patient);
            ActBean bean = new ActBean(appointment);
            bean.addParticipation("participation.clinician", clinician);
            bean.setValue("arrivalTime", arrivalTime);
            appointments[i] = appointment;
            bean.save();
        }

        ScheduleService service = (ScheduleService) applicationContext.getBean(
                "appointmentService");
        List<PropertySet> results = service.getEvents(schedule, date);
        assertEquals(count, results.size());
        for (int i = 0; i < results.size(); ++i) {
            PropertySet set = results.get(i);
            checkAppointment(appointments[i], set);
        }
    }

    /**
     * Tests moving of an event from one date to another.
     */
    @Test
    public void testChangeEventDate() {
        Date date1 = java.sql.Date.valueOf("2008-1-1");
        Date date2 = java.sql.Date.valueOf("2008-3-1");

        service.getEvents(schedule, date1);
        assertEquals(0, service.getEvents(schedule, date1).size());
        assertEquals(0, service.getEvents(schedule, date2).size());

        Act act = createAppointment(date1);

        assertEquals(1, service.getEvents(schedule, date1).size());
        assertEquals(0, service.getEvents(schedule, date2).size());

        act.setActivityStartTime(date2); // move it to date2
        act.setActivityEndTime(DateRules.getDate(date2, 15, DateUnits.MINUTES));
        getArchetypeService().save(act);

        assertEquals(0, service.getEvents(schedule, date1).size());
        assertEquals(1, service.getEvents(schedule, date2).size());
    }

    /**
     * Tests moving of an event from one schedule to another.
     */
    @Test
    public void testChangeEventSchedule() {
        Date date = java.sql.Date.valueOf("2008-1-1");

        service.getEvents(schedule, date);
        assertEquals(0, service.getEvents(schedule, date).size());

        Act act = createAppointment(date);
        assertEquals(1, service.getEvents(schedule, date).size());

        Party schedule2 = ScheduleTestHelper.createSchedule();
        ActBean bean = new ActBean(act);
        bean.setParticipant("participation.schedule", schedule2);

        getArchetypeService().save(act);

        assertEquals(0, service.getEvents(schedule, date).size());
        assertEquals(1, service.getEvents(schedule2, date).size());
    }


    /**
     * Sets up the test case.
     */
    @Before
    public void setUp() {
        service = (ScheduleService) applicationContext.getBean("appointmentService");
        schedule = ScheduleTestHelper.createSchedule();
    }

    /**
     * Verifies that an appointment matches the {@link PropertySet} representing
     * it.
     *
     * @param act the appointment
     * @param set the set
     */
    private void checkAppointment(Act act, PropertySet set) {
        ActBean bean = new ActBean(act);
        assertEquals(act.getObjectReference(),
                     set.get(ScheduleEvent.ACT_REFERENCE));
        assertEquals(act.getActivityStartTime(),
                     set.get(ScheduleEvent.ACT_START_TIME));
        assertEquals(act.getActivityEndTime(),
                     set.get(ScheduleEvent.ACT_END_TIME));
        assertEquals(act.getStatus(), set.get(ScheduleEvent.ACT_STATUS));
        assertEquals(TestHelper.getLookupName(act, "status"),
                     set.get(ScheduleEvent.ACT_STATUS_NAME));
        assertEquals(act.getReason(), set.get(ScheduleEvent.ACT_REASON));
        assertEquals(TestHelper.getLookupName(act, "reason"),
                     set.get(ScheduleEvent.ACT_REASON_NAME));
        assertEquals(act.getDescription(),
                     set.get(ScheduleEvent.ACT_DESCRIPTION));
        assertEquals(bean.getNodeParticipantRef("customer"),
                     set.get(ScheduleEvent.CUSTOMER_REFERENCE));
        assertEquals(bean.getNodeParticipant("customer").getName(),
                     set.get(ScheduleEvent.CUSTOMER_NAME));
        assertEquals(bean.getNodeParticipantRef("patient"),
                     set.get(ScheduleEvent.PATIENT_REFERENCE));
        assertEquals(bean.getNodeParticipant("patient").getName(),
                     set.get(ScheduleEvent.PATIENT_NAME));
        assertEquals(bean.getNodeParticipantRef("clinician"),
                     set.get(ScheduleEvent.CLINICIAN_REFERENCE));
        assertEquals(bean.getNodeParticipant("clinician").getName(),
                     set.get(ScheduleEvent.CLINICIAN_NAME));
        assertEquals(bean.getNodeParticipantRef("appointmentType"),
                     set.get(ScheduleEvent.SCHEDULE_TYPE_REFERENCE));
        assertEquals(bean.getNodeParticipantRef("schedule"),
                     set.get(ScheduleEvent.SCHEDULE_REFERENCE));
        assertEquals(bean.getNodeParticipant("schedule").getName(),
                     set.get(ScheduleEvent.SCHEDULE_NAME));
        assertEquals(bean.getNodeParticipant("appointmentType").getName(),
                     set.get(ScheduleEvent.SCHEDULE_TYPE_NAME));
        assertEquals(bean.getDate("arrivalTime"),
                     set.get(ScheduleEvent.ARRIVAL_TIME));
    }

    /**
     * Creates and saves a new appointment.
     *
     * @param date the date to create the appointment on
     * @return a new appointment
     */
    private Act createAppointment(Date date) {
        return createAppointment(date, schedule);
    }

    /**
     * Creates and saves a new appointment.
     *
     * @param date     the date to create the appointment on
     * @param schedule the schedule
     * @return a new appointment
     */
    private Act createAppointment(Date date, Party schedule) {
        Date startTime = DateRules.getDate(date, 15, DateUnits.MINUTES);
        Date endTime = DateRules.getDate(startTime, 15, DateUnits.MINUTES);
        return createAppointment(startTime, endTime, schedule);
    }

    /**
     * Creates and saves a new appointment.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @param schedule  the schedule
     * @return a new appointment
     */
    private Act createAppointment(Date startTime, Date endTime,
                                  Party schedule) {
        Party customer = TestHelper.createCustomer();
        Party patient = TestHelper.createPatient();
        User clinician = TestHelper.createClinician();
        Act appointment = ScheduleTestHelper.createAppointment(
                startTime, endTime, schedule, customer, patient, clinician);
        save(appointment);
        return appointment;
    }

}