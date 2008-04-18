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

package org.openvpms.archetype.rules.supplier;

import org.openvpms.archetype.rules.party.SupplierRules;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.service.archetype.helper.EntityBean;

import java.util.Date;


/**
 * Tests the {@link SupplierRules} class.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class SupplierRulesTestCase extends ArchetypeServiceTest {

    /**
     * The rules.
     */
    private SupplierRules rules;


    /**
     * Tests the {@link SupplierRules#getReferralVetPractice} method.
     */
    public void testGetReferralVet() {
        Party vet = TestHelper.createSupplierVet();

        Party practice = (Party) create("party.supplierVeterinaryPractice");
        practice.setName("XSupplierVeterinaryPractice");
        EntityBean bean = new EntityBean(practice);
        bean.addRelationship("entityRelationship.practiceVeterinarians", vet);
        bean.save();
        vet = get(vet); // reload to get relationship update
        EntityRelationship relationship
                = vet.getEntityRelationships().iterator().next();

        // verify the practice is returned for a time > the default start time
        Party practice2 = rules.getReferralVetPractice(vet, new Date());
        assertEquals(practice, practice2);

        // now set the start and end time and verify that there is no practice
        // for a later time (use time addition due to system clock granularity)
        Date start = new Date();
        Date end = new Date(start.getTime() + 1);
        Date later = new Date(end.getTime() + 1);
        relationship.setActiveStartTime(start);
        relationship.setActiveEndTime(end);
        assertNull(rules.getReferralVetPractice(vet, later));
    }

    /**
     * Sets up the test case.
     *
     * @throws Exception for any error
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        rules = new SupplierRules(getArchetypeService());
    }

}