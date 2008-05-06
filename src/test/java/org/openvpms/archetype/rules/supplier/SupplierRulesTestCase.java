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

import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.helper.EntityBean;

import java.util.Date;
import java.util.List;


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
     * Tests the {@link SupplierRules#isSuppliedBy(Party, Product)} method.
     */
    public void testIsSuppliedBy() {
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();

        assertFalse(rules.isSuppliedBy(supplier, product1));
        assertFalse(rules.isSuppliedBy(supplier, product2));

        ProductSupplier relationship
                = rules.createProductSupplier(product1, supplier);
        assertNotNull(relationship);

        assertTrue(rules.isSuppliedBy(supplier, product1));
        assertFalse(rules.isSuppliedBy(supplier, product2));
    }

    /**
     * Tests the {@link SupplierRules#getProductSuppliers(Party)} method.
     */
    public void testGetProductSuppliersForSupplier() {
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();

        ProductSupplier rel1 = rules.createProductSupplier(product1, supplier);
        ProductSupplier rel2 = rules.createProductSupplier(product2, supplier);

        List<ProductSupplier> relationships
                = rules.getProductSuppliers(supplier);
        assertEquals(2, relationships.size());
        assertTrue(relationships.contains(rel1));
        assertTrue(relationships.contains(rel2));

        // deactivate one of the relationships, and verify it is no longer
        // returned
        deactivateRelationship(rel1);

        relationships = rules.getProductSuppliers(supplier);
        assertEquals(1, relationships.size());
        assertFalse(relationships.contains(rel1));
        assertTrue(relationships.contains(rel2));
    }

    /**
     * Tests the {@link SupplierRules#getProductSuppliers(Party,Product)}
     * method.
     */
    public void testGetProductSuppliersForSupplierAndProduct() {
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();

        ProductSupplier p1rel1
                = rules.createProductSupplier(product1, supplier);
        ProductSupplier p1rel2
                = rules.createProductSupplier(product1, supplier);
        ProductSupplier p2rel1
                = rules.createProductSupplier(product2, supplier);

        List<ProductSupplier> relationships
                = rules.getProductSuppliers(supplier, product1);
        assertEquals(2, relationships.size());
        assertTrue(relationships.contains(p1rel1));
        assertTrue(relationships.contains(p1rel2));
        assertFalse(relationships.contains(p2rel1));

        // deactivate one of the relationships, and verify it is no longer
        // returned. Need to sleep to allow for system clock granularity
        deactivateRelationship(p1rel1);

        relationships = rules.getProductSuppliers(supplier, product1);
        assertEquals(1, relationships.size());
        assertFalse(relationships.contains(p1rel1));
        assertTrue(relationships.contains(p1rel2));
    }

    /**
     * Tests the {@link SupplierRules#getProductSupplier(Party, Product, int,
     * String)} method.
     */
    public void testGetProductSupplier() {
        Party supplier = TestHelper.createSupplier();
        Product product1 = TestHelper.createProduct();
        Product product2 = TestHelper.createProduct();

        // create some relationships
        ProductSupplier p1rel1
                = rules.createProductSupplier(product1, supplier);
        ProductSupplier p1rel2
                = rules.createProductSupplier(product1, supplier);
        ProductSupplier p2rel
                = rules.createProductSupplier(product2, supplier);

        assertEquals(0, p1rel1.getPackageSize()); // default value
        p1rel2.setPackageSize(3);
        p1rel2.setPackageUnits("AMPOULE");
        p2rel.setPackageSize(4);
        p2rel.setPackageUnits("PACKET");

        // verify that p1rel is returned if there is no corresponding
        // relationship, as its package size isn't set
        ProductSupplier test1
                = rules.getProductSupplier(supplier, product1, 4, "BOX");
        assertEquals(p1rel1, test1);

        p1rel1.setPackageSize(4);
        p1rel1.setPackageUnits("BOX");

        // verify that the correct relationship is returned for exact matches
        assertEquals(p1rel1, rules.getProductSupplier(supplier, product1, 4,
                                                      "BOX"));
        assertEquals(p1rel2, rules.getProductSupplier(supplier, product1, 3,
                                                      "AMPOULE"));
        assertEquals(p2rel, rules.getProductSupplier(supplier, product2, 4,
                                                     "PACKET"));

        // verify that nothing is returned if there is no direct match
        assertNull(rules.getProductSupplier(supplier, product1, 5, "BOX"));
        assertNull(rules.getProductSupplier(supplier, product1, 5, "PACKET"));
        assertNull(rules.getProductSupplier(supplier, product2, 5, "PACKET"));
    }

    /**
     * Tests the {@link SupplierRules#getProductSuppliers(Product)} method.
     */
    public void testGetProductSuppliersForProduct() {
        Party supplier1 = TestHelper.createSupplier();
        Party supplier2 = TestHelper.createSupplier();
        Product product = TestHelper.createProduct();

        ProductSupplier rel1 = rules.createProductSupplier(product, supplier1);
        ProductSupplier rel2 = rules.createProductSupplier(product, supplier2);

        List<ProductSupplier> relationships
                = rules.getProductSuppliers(product);
        assertEquals(2, relationships.size());
        assertTrue(relationships.contains(rel1));
        assertTrue(relationships.contains(rel2));

        // deactivate one of the relationships, and verify it is no longer
        // returned
        deactivateRelationship(rel1);

        relationships = rules.getProductSuppliers(product);
        assertEquals(1, relationships.size());
        assertFalse(relationships.contains(rel1));
        assertTrue(relationships.contains(rel2));
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

    /**
     * Helper to deactivate a relationship and sleep for a second, so that
     * subsequent isActive() checks return false. The sleep in between
     * deactivating a relationship and calling isActive() is required due to
     * system time granularity.
     *
     * @param relationship the relationship to deactivate
     */
    private void deactivateRelationship(ProductSupplier relationship) {
        relationship.getRelationship().setActive(false);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
            // do nothing
        }
    }

}
