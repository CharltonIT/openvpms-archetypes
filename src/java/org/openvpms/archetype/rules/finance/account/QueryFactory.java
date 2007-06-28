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

package org.openvpms.archetype.rules.finance.account;

import static org.openvpms.archetype.rules.finance.account.CustomerAccountActTypes.ACCOUNT_BALANCE_SHORTNAME;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.system.common.query.ArchetypeQuery;
import org.openvpms.component.system.common.query.CollectionNodeConstraint;
import org.openvpms.component.system.common.query.NodeSelectConstraint;
import org.openvpms.component.system.common.query.NodeSortConstraint;
import org.openvpms.component.system.common.query.ObjectRefNodeConstraint;
import org.openvpms.component.system.common.query.RelationalOp;
import org.openvpms.component.system.common.query.ShortNameConstraint;


/**
 * Helper to create queries for customer account acts.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
class QueryFactory {

    /**
     * Creates an object set query for unallocated acts for the specified
     * customer. Returns only the amount, allocatedAmount and credit nodes,
     * named <em>a.amount</em>, <em>a.allocatedAmount</em> and <em>a.credit</em>
     * respectively.
     *
     * @param customer   the customer
     * @param shortNames the act short names
     * @return a new query
     */
    public static ArchetypeQuery createUnallocatedObjectSetQuery(
            Party customer, String[] shortNames) {
        ArchetypeQuery query = createUnallocatedQuery(customer, shortNames,
                                                      null);
        query.add(new NodeSelectConstraint("a.amount"));
        query.add(new NodeSelectConstraint("a.allocatedAmount"));
        query.add(new NodeSelectConstraint("a.credit"));
        return query;
    }

    /**
     * Creates a query for unallocated acts for the specified customer.
     *
     * @param customer   the customer
     * @param shortNames the act short names
     * @param exclude    the act to exclude. May be <tt>null</tt>
     * @return a new query
     */
    public static ArchetypeQuery createUnallocatedQuery(Party customer,
                                                        String[] shortNames,
                                                        Act exclude) {
        ShortNameConstraint archetypes
                = new ShortNameConstraint("a", shortNames, false, false);
        ArchetypeQuery query = new ArchetypeQuery(archetypes);
        CollectionNodeConstraint constraint = new CollectionNodeConstraint(
                "accountBalance", ACCOUNT_BALANCE_SHORTNAME, false, false);
        constraint.add(new ObjectRefNodeConstraint(
                "entity", customer.getObjectReference()));
        if (exclude != null) {
            constraint.add(new ObjectRefNodeConstraint(
                    "act", RelationalOp.NE, exclude.getObjectReference()));
        }
        query.add(constraint);
        query.add(new NodeSortConstraint("startTime", false));
        return query;
    }

    public static ArchetypeQuery createObjectSetQuery(Party customer,
                                                      String[] shortNames,
                                                      boolean sortAscending) {
        ArchetypeQuery query = createQuery(customer, shortNames);
        query.add(new NodeSortConstraint("startTime", sortAscending));
        query.add(new NodeSelectConstraint("a.amount"));
        query.add(new NodeSelectConstraint("a.credit"));
        return query;
    }

    /**
     * Creates a query for all acts matching the specified short names,
     * for a customer.
     *
     * @param customer   the customer
     * @param shortNames the act archetype short names
     * @return the corresponding query
     */
    public static ArchetypeQuery createQuery(Party customer,
                                             String[] shortNames) {
        ShortNameConstraint archetypes
                = new ShortNameConstraint("a", shortNames, false, false);
        ArchetypeQuery query = new ArchetypeQuery(archetypes);
        CollectionNodeConstraint constraint = new CollectionNodeConstraint(
                "customer", "participation.customer", false, false);
        constraint.add(new ObjectRefNodeConstraint(
                "entity", customer.getObjectReference()));
        query.add(constraint);
        return query;
    }

}
