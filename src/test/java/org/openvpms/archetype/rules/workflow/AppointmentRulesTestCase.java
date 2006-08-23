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

package org.openvpms.archetype.rules.workflow;

import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.ArchetypeQueryHelper;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.system.common.query.ArchetypeQuery;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * Tests the {@link AppointmentRules} class.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class AppointmentRulesTestCase extends ArchetypeServiceTest {

    /**
     * Tests the behaviour of {@link AppointmentRules#calculateEndTime} when
     * the schedule units are in minutes .
     */
    public void testCalculateEndTimeForMinsUnits() {
        Entity appointmentType = createAppointmentType();
        save(appointmentType);
        Party schedule = createSchedule(15, "minutes", 2, appointmentType);
        Date start = createTime(9, 0);
        Date end = AppointmentRules.calculateEndTime(start, schedule,
                                                     appointmentType);
        Date expected = createTime(9, 30);
        assertEquals(expected, end);
    }

    /**
     * Tests the behaviour of {@link AppointmentRules#calculateEndTime} when
     * the schedule units are in hours.
     */
    public void testCalculateEndTimeForHoursUnits() {
        Entity appointmentType = createAppointmentType();
        save(appointmentType);
        Party schedule = createSchedule(1, "hours", 3, appointmentType);
        Date start = createTime(9, 0);
        Date end = AppointmentRules.calculateEndTime(start, schedule,
                                                     appointmentType);
        Date expected = createTime(12, 0);
        assertEquals(expected, end);
    }

    /**
     * Tests the behaviour of
     * {@link AppointmentRules#hasOverlappingAppointments}.
     */
    public void testHasOverlappingAppointments() {
        Date start = createTime(9, 0);
        Date end = createTime(9, 15);
        Act appointment = createAppointment(start, end);
        assertFalse(AppointmentRules.hasOverlappingAppointments(appointment));
        save(appointment);
        assertFalse(AppointmentRules.hasOverlappingAppointments(appointment));

        Act exactOverlap = createAppointment(start, end);
        assertTrue(AppointmentRules.hasOverlappingAppointments(exactOverlap));

        Act overlap = createAppointment(createTime(9, 5), createTime(9, 10));
        assertTrue(AppointmentRules.hasOverlappingAppointments(overlap));

        Act after = createAppointment(createTime(9, 15), createTime(9, 30));
        assertFalse(AppointmentRules.hasOverlappingAppointments(after));

        Act before = createAppointment(createTime(8, 45), createTime(9, 0));
        assertFalse(AppointmentRules.hasOverlappingAppointments(before));
    }

    /**
     * Tests the behaviour of {@link AppointmentRules#hasOverlappingAppointments}
     * for an unpopulated appointment.
     */
    public void testHasOverlappingAppointmentsForEmptyAct() {
        Date start = createTime(9, 0);
        Date end = createTime(9, 15);
        Act appointment = createAppointment(start, end);
        save(appointment);

        Act empty = createAct("act.customerAppointment");
        empty.setActivityStartTime(null);
        empty.setActivityEndTime(null);

        assertFalse(AppointmentRules.hasOverlappingAppointments(empty));
    }

    /**
     * Sets up the test case.
     *
     * @throws Exception for any error
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        removeActs();
    }

    /**
     * Helper to create an <em>act.customerAppointment</em>.
     *
     * @param startTime the act start time
     * @param endTime   the act end time
     * @return a new act
     */
    protected Act createAppointment(Date startTime, Date endTime) {
        Act act = createAct("act.customerAppointment");
        ActBean bean = new ActBean(act);
        bean.setValue("startTime", startTime);
        bean.setValue("endTime", endTime);
        Party customer = (Party) create("party.customerperson");
        Party schedule = (Party) create("party.organisationSchedule");
        Entity appointmentType = (Entity) create("entity.appointmentType");
        bean.setParticipant("participation.customer", customer);
        bean.setParticipant("participation.schedule", schedule);
        bean.setParticipant("participation.appointmentType", appointmentType);
        return act;
    }

    /**
     * Helper to create a new act.
     *
     * @param shortName the act short name
     * @return a new act
     */
    protected Act createAct(String shortName) {
        return (Act) create(shortName);
    }

    /**
     * Helper to create a new <em>entity.appointmentType</em>.
     *
     * @return a new appointment type
     */
    protected Entity createAppointmentType() {
        Entity appointmentType = (Entity) create("entity.appointmentType");
        appointmentType.setName("XAppointmentType");
        return appointmentType;
    }

    /**
     * Helper to create a new <code>party.organisationSchedule</em>.
     *
     * @param slotSize        the schedule slot size
     * @param slotUnits       the schedule slot units
     * @param noSlots         the appointment no. of slots
     * @param appointmentType the appointment type
     * @return a new schedule
     */
    protected Party createSchedule(int slotSize, String slotUnits,
                                   int noSlots, Entity appointmentType) {
        Party schedule = (Party) create("party.organisationSchedule");
        EntityBean bean = new EntityBean(schedule);
        bean.setValue("slotSize", slotSize);
        bean.setValue("slotUnits", slotUnits);
        EntityRelationship relationship = (EntityRelationship) create(
                "entityRelationship.scheduleAppointmentType");
        relationship.setSource(schedule.getObjectReference());
        relationship.setTarget(appointmentType.getObjectReference());
        IMObjectBean relBean = new IMObjectBean(relationship);
        relBean.setValue("noSlots", noSlots);
        schedule.addEntityRelationship(relationship);
        return schedule;
    }

    /**
     * Helper to create a time with a fixed date.
     *
     * @param hour    the hour
     * @param minutes the minutes
     * @return a new time
     */
    private Date createTime(int hour, int minutes) {
        Calendar calendar = new GregorianCalendar(2006, 8, 22, hour, minutes);
        return calendar.getTime();
    }

    /**
     * Remove any existing appointment acts that will interfere with the tests.
     */
    private void removeActs() {
        Date startDay = createTime(0, 0);
        Date endDay = createTime(23, 59);
        List rows = ArchetypeQueryHelper.getActs(
                getArchetypeService(), "act", "customerAppointment",
                startDay, endDay, null, null, null, true, 0,
                ArchetypeQuery.ALL_ROWS).getRows();
        for (Object object : rows) {
            Act act = (Act) object;
            remove(act);
        }
    }

}