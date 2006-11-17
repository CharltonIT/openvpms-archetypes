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

package org.openvpms.archetype.rules.patient.reminder;

import org.openvpms.component.business.domain.im.common.Entity;

import java.util.HashMap;
import java.util.Map;


/**
 * Reminder statistics.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class Statistics {

    /**
     * Reminder processing type.
     */
    public enum Type {
        SKIPPED, CANCELLED, PRINTED, EMAILED, LISTED}

    /**
     * Tracks statistics by reminder type.
     */
    private final Map<Entity, Map<Type, Integer>> statistics
            = new HashMap<Entity, Map<Type, Integer>>();


    /**
     * Increments the count for a reminder type.
     *
     * @param reminderType the reminderType
     * @param type         the processing type
     */
    public void increment(Entity reminderType, Type type) {
        Map<Type, Integer> stats = statistics.get(reminderType);
        if (stats == null) {
            stats = new HashMap<Type, Integer>();
            statistics.put(reminderType, stats);
        }
        Integer count = stats.get(type);
        if (count == null) {
            stats.put(type, 0);
        } else {
            stats.put(type, count + 1);
        }
    }

    /**
     * Returns the count for a processing type.
     *
     * @param type the processing type
     * @return the count for the processing type
     */
    public int getCount(Type type) {
        int result = 0;
        for (Map<Type, Integer> stats : statistics.values()) {
            Integer count = stats.get(type);
            if (count != null) {
                result += count;
            }
        }
        return result;
    }

}