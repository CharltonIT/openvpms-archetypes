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

package org.openvpms.archetype.rules.util;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


/**
 * Date rules.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class DateRules {

    /**
     * Calculates a date given a start time, interval and the date units.
     *
     * @param startTime the start time
     * @param interval  the time interval. May be negative to calculate a date
     *                  in the past
     * @param units     the interval units
     * @return the date
     */
    public static Date getDate(Date startTime, int interval, DateUnits units) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(startTime);
        if (units != null) {
            switch (units) {
                case YEARS:
                    calendar.add(Calendar.YEAR, interval);
                    break;
                case MONTHS:
                    calendar.add(Calendar.MONTH, interval);
                    break;
                case WEEKS:
                    calendar.add(Calendar.DAY_OF_YEAR, interval * 7);
                    break;
                case DAYS:
                    calendar.add(Calendar.DAY_OF_YEAR, interval);
                    break;
            }
        }
        return calendar.getTime();
    }

    /**
     * Helper to compare two dates.
     * <p/>
     * This is functionally equivalent to the {@link Date#compareTo(Date)}
     * method, except that it doesn't throw <tt>ClassCastExceptions</tt>
     * if <tt>lhs</tt> is an instance of a {@link Timestamp Timestamp} and
     * <tt>rhs</tt> isn't.
     * <p/>
     * For timestamps, the nanoseconds are ignored.
     *
     * @param lhs the date
     * @param rhs the date to compare with
     * @return <tt>0</tt> if the <tt>lhs</tt> is equal to <tt>rhs</tt>;
     *          a value less than <tt>0</tt> if <tt>lhs</tt> is before
     *         <tt>rhs</tt>; and a value greater than
     *      <tt>0</tt> if <tt>lhs</tt> is after <tt>rhs</tt>.
     */
    public static int compareTo(Date lhs, Date rhs) {
        long lhsTime = lhs.getTime();
        long rhsTime = rhs.getTime();
        return (lhsTime < rhsTime ? -1 : (lhsTime == rhsTime ? 0 : 1));
    }
}
