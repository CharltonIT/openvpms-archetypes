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

package org.openvpms.archetype.rules.workflow;

import net.sf.ehcache.Cache;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.lookup.ILookupService;
import org.openvpms.component.system.common.util.PropertySet;

import java.util.Date;


/**
 * Implementation of the {@link ScheduleService} for task events.
 *
 * @author Tim Anderson
 */
public class TaskService extends AbstractScheduleService {

    /**
     * Constructs a {@link TaskService}.
     *
     * @param service the archetype service
     * @param cache   the cache
     */
    public TaskService(IArchetypeService service, ILookupService lookupService, Cache cache) {
        super(ScheduleArchetypes.TASK, service, lookupService, cache);
    }

    /**
     * Assembles an {@link PropertySet PropertySet} from a source act.
     *
     * @param target the target set
     * @param source the source act
     */
    @Override
    protected void assemble(PropertySet target, ActBean source) {
        super.assemble(target, source);

        IMObjectReference scheduleRef = source.getNodeParticipantRef("worklist");
        String scheduleName = getName(scheduleRef);
        target.set(ScheduleEvent.SCHEDULE_REFERENCE, scheduleRef);
        target.set(ScheduleEvent.SCHEDULE_NAME, scheduleName);

        IMObjectReference typeRef = source.getNodeParticipantRef("taskType");
        String typeName = getName(typeRef);
        target.set(ScheduleEvent.SCHEDULE_TYPE_REFERENCE, typeRef);
        target.set(ScheduleEvent.SCHEDULE_TYPE_NAME, typeName);

        String reason = source.getAct().getReason();
        target.set(ScheduleEvent.ACT_REASON, reason);
        target.set(ScheduleEvent.ACT_REASON_NAME, reason);

        target.set(ScheduleEvent.CONSULT_START_TIME, source.getDate(ScheduleEvent.CONSULT_START_TIME));
    }

    /**
     * Returns the schedule reference from an event.
     *
     * @param event the event
     * @return a reference to the schedule. May be {@code null}
     */
    protected IMObjectReference getSchedule(Act event) {
        ActBean bean = new ActBean(event, getService());
        return bean.getNodeParticipantRef("worklist");
    }

    /**
     * Creates a new query to query events for the specified schedule and date range.
     *
     * @param schedule the schedule
     * @param from     the start time
     * @param to       the end time
     * @return a new query
     */
    protected ScheduleEventQuery createQuery(Entity schedule, Date from, Date to) {
        return new TaskQuery((Party) schedule, from, to, getService());
    }

}
