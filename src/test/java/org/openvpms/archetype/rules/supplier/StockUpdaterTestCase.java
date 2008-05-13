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

package org.openvpms.archetype.rules.supplier;

import org.openvpms.archetype.rules.act.ActStatus;
import org.openvpms.archetype.rules.finance.account.FinancialTestHelper;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.archetype.test.TestHelper;
import org.openvpms.component.business.domain.im.act.FinancialAct;
import org.openvpms.component.business.domain.im.common.EntityRelationship;
import org.openvpms.component.business.domain.im.datatypes.quantity.Money;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.service.archetype.helper.ActBean;
import org.openvpms.component.business.service.archetype.helper.EntityBean;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;

import java.math.BigDecimal;
import java.util.List;


/**
 * Tests the {@link StockUpdater} class, when invoked by the
 * <em>archetypeService.save.act.customerAccountChargesInvoice.before</em>,
 * <em>archetypeService.save.act.customerAccountChargesCredit.before</em> and
 * <em>archetypeService.save.act.customerAccountChargesCounter.before</em>
 * rules.
 *
 * @author <a href="mailto:support@openvpms.org">OpenVPMS Team</a>
 * @version $LastChangedDate: 2006-05-02 05:16:31Z $
 */
public class StockUpdaterTestCase extends ArchetypeServiceTest {

    /**
     * The organisation location.
     */
    private Party location;

    /**
     * The product.
     */
    private Product product;

    /**
     * The customer.
     */
    private Party customer;

    /**
     * The patient.
     */
    private Party patient;

    /**
     * The stock location.
     */
    private Party stockLocation;


    /**
     * Verifies that stock is updated when an invoice, counter and credit
     * charge is posted.
     * <p/>
     * Note that the quantity can go negative to reflect the fact that
     * a delivery may be processed through after the stock itself is actually
     * used.
     */
    public void testStockUpdate() {
        BigDecimal initialQuantity = BigDecimal.ZERO;
        BigDecimal invoiceQuantity = BigDecimal.valueOf(5);
        BigDecimal counterQuantity = BigDecimal.valueOf(20);
        BigDecimal creditQuantity = BigDecimal.valueOf(30);

        // add a relationship between the location and stock location
        EntityBean locBean = new EntityBean(location);
        locBean.addRelationship("entityRelationship.locationStockLocation",
                                stockLocation);
        locBean.save();
        save(stockLocation);

        // verify the stock when an invoice is saved
        List<FinancialAct> invoice = FinancialTestHelper.createChargesInvoice(
                new Money(100), customer, patient, product,
                ActStatus.IN_PROGRESS);
        BigDecimal expected = initialQuantity.subtract(invoiceQuantity);
        checkStockUpdate(invoice, invoiceQuantity, expected);

        // verify the stock when a counter charge is saved
        List<FinancialAct> counter = FinancialTestHelper.createChargesCounter(
                new Money(90), customer, product, ActStatus.IN_PROGRESS);
        expected = expected.subtract(counterQuantity);
        checkStockUpdate(counter, counterQuantity, expected);

        // verify the stock when a credit is saved
        List<FinancialAct> credit = FinancialTestHelper.createChargesCredit(
                new Money(100), customer, patient, product,
                ActStatus.IN_PROGRESS);
        expected = expected.add(creditQuantity);
        checkStockUpdate(credit, creditQuantity, expected);
    }

    /**
     * Verifies that there are no stock changes if the location doesn't have
     * an associated stock location.
     */
    public void testStockUpdateForNoStockLocation() {
        BigDecimal expected = BigDecimal.ZERO;
        BigDecimal quantity = BigDecimal.valueOf(100);

        // verify the stock when an invoice is saved
        List<FinancialAct> invoice = FinancialTestHelper.createChargesInvoice(
                new Money(100), customer, patient, product,
                ActStatus.IN_PROGRESS);
        checkStockUpdate(invoice, quantity, expected);

        // verify the stock when a counter charge is saved
        List<FinancialAct> counter = FinancialTestHelper.createChargesCounter(
                new Money(90), customer, product, ActStatus.IN_PROGRESS);
        checkStockUpdate(counter, quantity, expected);

        // verify the stock when a credit is saved
        List<FinancialAct> credit = FinancialTestHelper.createChargesCredit(
                new Money(100), customer, patient, product,
                ActStatus.IN_PROGRESS);
        checkStockUpdate(credit, quantity, expected);
    }

    /**
     * Sets up the test case.
     *
     * @throws Exception for any error
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        location = TestHelper.createLocation();
        product = TestHelper.createProduct();
        customer = TestHelper.createCustomer();
        patient = TestHelper.createPatient();
        stockLocation = (Party) create(SupplierArchetypes.STOCK_LOCATION);
        stockLocation.setName("STOCK-LOCATION-" + stockLocation.hashCode());
        save(stockLocation);
    }

    /**
     * Verifies the stock quantities before and after a charge is posted.
     *
     * @param acts     the charge acts
     * @param quantity the charge item quantity
     * @param expected the expected quantity after the charge is posted
     */
    private void checkStockUpdate(List<FinancialAct> acts, BigDecimal quantity,
                                  BigDecimal expected) {
        FinancialAct act = acts.get(0);
        FinancialAct item = acts.get(1);
        ActBean bean = new ActBean(act);
        bean.addParticipation("participation.location", location);
        item.setQuantity(quantity);

        // saving un-POSTED act shouldn't cause stock update
        assertFalse(ActStatus.POSTED.equals(act.getStatus()));
        BigDecimal current = getStock();
        save(acts);
        assertEquals(current, getStock());

        // now post the act
        act.setStatus(ActStatus.POSTED);
        save(act);

        assertEquals(expected, getStock());
    }

    /**
     * Returns the stock in hand for the product.
     *
     * @return the stock in hand
     */
    private BigDecimal getStock() {
        product = get(product);
        EntityBean prodBean = new EntityBean(product);
        EntityRelationship rel = prodBean.getRelationship(stockLocation);
        if (rel != null) {
            IMObjectBean relBean = new IMObjectBean(rel);
            return relBean.getBigDecimal("quantity");
        }
        return BigDecimal.ZERO;
    }


}
