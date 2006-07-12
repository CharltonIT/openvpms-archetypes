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

package org.openvpms.archetype.rules.till;

import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.common.IMObject;
import org.openvpms.component.business.domain.im.common.IMObjectReference;
import org.openvpms.component.business.domain.im.datatypes.quantity.Money;
import org.openvpms.component.business.service.archetype.ArchetypeServiceHelper;
import org.openvpms.component.business.service.archetype.IArchetypeService;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.CollectionNodeConstraint;
import org.openvpms.component.system.common.query.NodeConstraint;
import org.openvpms.component.system.common.query.ObjectRefNodeConstraint;
import org.openvpms.component.system.common.query.RelationalOp;

import java.util.List;


/**
 * Till helper.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class TillHelper {

    /**
     * Helper to return the uncleared till balance for a till, if it exists.
     *
     * @param till a reference to the till
     * @return the uncleared till balance, or <code>null</code> if none exists
     */
    public static Act getUnclearedTillBalance(IMObjectReference till) {
        Act act = null;
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        ArchetypeQuery query = new ArchetypeQuery(TillRules.TILL_BALANCE,
                                                  false,
                                                  true);
        query.setFirstRow(0);
        query.setNumOfRows(ArchetypeQuery.ALL_ROWS);
        query.add(new NodeConstraint("status", RelationalOp.EQ,
                                     TillRules.UNCLEARED));
        CollectionNodeConstraint participations
                = new CollectionNodeConstraint("till",
                                               TillRules.TILL_PARTICIPATION,
                                               false, true);
        participations.add(new ObjectRefNodeConstraint("entity", till));
        query.add(participations);
        List<IMObject> matches = service.get(query).getRows();
        if (!matches.isEmpty()) {
            act = (Act) matches.get(0);
        }
        return act;
    }

    /**
     * Helper to create a new till balance, associating it with a till.
     *
     * @param till the till
     * @return a new till balance
     */
    public static Act createTillBalance(IMObjectReference till) {
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        Act act = (Act) service.create(TillRules.TILL_BALANCE);
        ActBean bean = new ActBean(act);
        bean.setStatus(TillRules.UNCLEARED);
        bean.setParticipant(TillRules.TILL_PARTICIPATION, till);
        return act;
    }

    /**
     * Creates a new till balance adjustment, associating it with a till.
     *
     * @param till   the till
     * @param amount the amount
     * @return a new till balance adjustment
     */
    public static Act createTillBalanceAdjustment(
            IMObjectReference till, Money amount) {
        IArchetypeService service
                = ArchetypeServiceHelper.getArchetypeService();
        Act act = (Act) service.create("act.tillBalanceAdjustment");
        ActBean bean = new ActBean(act);
        bean.setValue("amount", amount);
        bean.setParticipant(TillRules.TILL_PARTICIPATION, till);
        return act;
    }

}
