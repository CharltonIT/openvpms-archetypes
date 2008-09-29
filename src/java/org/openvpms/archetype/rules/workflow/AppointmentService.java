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

import net.sf.ehcache.Cache;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.system.common.query.ObjectSet;

import java.util.Date;


/**
 * Implementation of the {@link ScheduleService} for appointment events.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class AppointmentService extends AbstractScheduleService {

    /**
     * Creates a new <tt>AppointmentServiceImpl</tt>.
     *
     * @param service the archetype service
     * @param cache   the cache
     */
    public AppointmentService(IArchetypeService service, Cache cache) {
        super("act.customerAppointment", service, cache);
    }

    @Override
    protected void assemble(ObjectSet target, ActBean source) {
        super.assemble(target, source);

        IMObjectReference scheduleRef
                = source.getNodeParticipantRef("schedule");
        String scheduleName = getName(scheduleRef);
        target.set(ScheduleEvent.SCHEDULE_REFERENCE, scheduleRef);
        target.set(ScheduleEvent.SCHEDULE_NAME, scheduleName);

        IMObjectReference typeRef
                = source.getNodeParticipantRef("appointmentType");
        String typeName = getName(typeRef);
        target.set(ScheduleEvent.SCHEDULE_TYPE_REFERENCE, typeRef);
        target.set(ScheduleEvent.SCHEDULE_TYPE_NAME, typeName);

        target.set(ScheduleEvent.ARRIVAL_TIME,
                   source.getDate(ScheduleEvent.ARRIVAL_TIME));
    }

    protected IMObjectReference getSchedule(Act event) {
        ActBean bean = new ActBean(event, getService());
        return bean.getNodeParticipantRef("schedule");
    }

    protected ScheduleEventQuery createQuery(Entity schedule, Date from,
                                             Date to) {
        return new AppointmentQuery((Party) schedule, from, to, getService());
    }
}
