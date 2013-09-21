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

package org.openvpms.archetype.rules.patient;

import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.Act;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.helper.ActBean;

/**
 * Patient test helper methods.
 *
 * @author Tim Anderson
 */
public class PatientTestHelper {

    /**
     * Helper to create an <em>act.patientMedication</em>.
     *
     * @param patient the patient
     * @return a new act
     */
    public static Act createMedication(Party patient) {
        return createMedication(patient, TestHelper.createProduct());
    }

    /**
     * Helper to create an <em>act.patientMedication</em>.
     *
     * @param patient the patient
     * @param product the product
     * @return a new act
     */
    public static Act createMedication(Party patient, Product product) {
        Act act = (Act) TestHelper.create(PatientArchetypes.PATIENT_MEDICATION);
        ActBean bean = new ActBean(act);
        bean.addNodeParticipation("patient", patient);
        bean.addNodeParticipation("product", product);
        bean.save();
        return act;
    }
}
