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

import org.openvpms.archetype.rules.util.DateRules;
import org.openvpms.archetype.rules.util.DateUnits;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.security.User;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.system.common.query.ObjectSet;

import java.util.Date;
import java.util.List;

/**
 * Tests the {@link TaskService}.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class TaskServiceTestCase extends ArchetypeServiceTest {

    /**
     * The task service.
     */
    private ScheduleService service;

    /**
     * The work list.
     */
    private Party workList;


    /**
     * Tests addition of a task.
     */
    public void testAddEvent() {
        Date date = java.sql.Date.valueOf("2008-1-1");

        List<ObjectSet> results = service.getEvents(workList, date);
        assertEquals(0, results.size());

        Act task = createTask(date);

        results = service.getEvents(workList, date);
        assertEquals(1, results.size());
        ObjectSet set = results.get(0);
        checkTask(task, set);
    }

    /**
     * Tests removal of an event.
     */
    public void testRemoveEvent() {
        Date date = java.sql.Date.valueOf("2008-1-1");

        List<ObjectSet> results = service.getEvents(workList, date);
        assertEquals(0, results.size());

        Act task = createTask(date);

        results = service.getEvents(workList, date);
        assertEquals(1, results.size());

        getArchetypeService().remove(task);

        assertEquals(0, service.getEvents(workList, date).size());
    }

    /**
     * Tests moving of an event from one date to another.
     */
    public void testMoveEvent() {
        Date date1 = java.sql.Date.valueOf("2008-1-1");
        Date date2 = java.sql.Date.valueOf("2008-3-1");

        service.getEvents(workList, date1);
        assertEquals(0, service.getEvents(workList, date1).size());
        assertEquals(0, service.getEvents(workList, date2).size());

        Act task = createTask(date1);

        assertEquals(1, service.getEvents(workList, date1).size());
        assertEquals(0, service.getEvents(workList, date2).size());

        task.setActivityStartTime(date2); // move it to date2
        getArchetypeService().save(task);

        assertEquals(0, service.getEvents(workList, date1).size());
        assertEquals(1, service.getEvents(workList, date2).size());
    }

    /**
     * Tests the {@link TaskService#getEvents(Entity, Date)} method.
     */
    public void testGetEvents() {
        final int count = 10;
        Party schedule = ScheduleTestHelper.createWorkList();
        Act[] tasks = new Act[count];
        Date date = java.sql.Date.valueOf("2007-1-1");
        for (int i = 0; i < count; ++i) {
            Date startTime = DateRules.getDate(date, 15 * count,
                                               DateUnits.MINUTES);
            Date endTime = DateRules.getDate(startTime, 15, DateUnits.MINUTES);

            tasks[i] = createTask(startTime, endTime, schedule);
        }

        List<ObjectSet> results = service.getEvents(schedule, date);
        assertEquals(count, results.size());
        for (int i = 0; i < results.size(); ++i) {
            checkTask(tasks[i], results.get(i));
        }
    }

    /**
     * Creates and saves a new task.
     *
     * @param date the date to create the task on
     * @return a new task
     */
    private Act createTask(Date date) {
        Date startTime = DateRules.getDate(date, 15, DateUnits.MINUTES);
        return createTask(startTime, null, workList);
    }

    /**
     * Creates and saves a new task.
     *
     * @param startTime the start time
     * @param endTime   the end time
     * @param workList  the work list
     * @return a new task
     */
    private Act createTask(Date startTime, Date endTime, Party workList) {
        Party customer = TestHelper.createCustomer();
        Party patient = TestHelper.createPatient();
        User clinician = TestHelper.createClinician();
        Act task = ScheduleTestHelper.createTask(
                startTime, endTime, workList, customer, patient, clinician);
        save(task);
        return task;
    }

    /**
     * Sets up the test case.
     *
     * @throws Exception for any error
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        service = (ScheduleService) applicationContext.getBean("taskService");
        workList = ScheduleTestHelper.createWorkList();
    }


    /**
     * Verifies that a task matches the {@link ObjectSet} representing it.
     *
     * @param task the task
     * @param set  the set
     */
    private void checkTask(Act task, ObjectSet set) {
        ActBean bean = new ActBean(task);
        assertEquals(task.getObjectReference(),
                     set.get(ScheduleEvent.ACT_REFERENCE));
        assertEquals(task.getActivityStartTime(),
                     set.get(ScheduleEvent.ACT_START_TIME));
        assertEquals(task.getActivityEndTime(),
                     set.get(ScheduleEvent.ACT_END_TIME));
        assertEquals(task.getStatus(), set.get(ScheduleEvent.ACT_STATUS));
        assertEquals(task.getReason(), set.get(ScheduleEvent.ACT_REASON));
        assertEquals(task.getDescription(),
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
        assertEquals(bean.getNodeParticipantRef("taskType"),
                     set.get(ScheduleEvent.SCHEDULE_TYPE_REFERENCE));
        assertEquals(bean.getNodeParticipant("taskType").getName(),
                     set.get(ScheduleEvent.SCHEDULE_TYPE_NAME));
    }

}