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
 *  Copyright 2007 (C) OpenVPMS Ltd. All Rights Reserved.
 *
 *  $Id$
 */

package org.openvpms.archetype.rules.patient.reminder;

import org.openvpms.archetype.component.processor.ProcessorListener;
import org.openvpms.component.business.service.archetype.ArchetypeServiceException;
import org.openvpms.component.business.service.archetype.IArchetypeService;


/**
 * A {@link ProcessorListener} that cancels reminders.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class ReminderCancelProcessor extends AbstractReminderProcessorListener {

    /**
     * Constructs a new <tt>ReminderCancelProcessor</tt>.
     */
    public ReminderCancelProcessor() {
    }

    /**
     * Constructs a new <tt>ReminderCancelProcessor</tt>.
     */
    public ReminderCancelProcessor(IArchetypeService service) {
        super(service);
    }

    /**
     * Invoked to process a reminder.
     *
     * @param event the event
     * @throws ArchetypeServiceException  for any archetype service error
     * @throws ReminderProcessorException if the event cannot be processed
     */
    public void process(ReminderEvent event) {
        getRules().cancelReminder(event.getReminder());
    }
}
