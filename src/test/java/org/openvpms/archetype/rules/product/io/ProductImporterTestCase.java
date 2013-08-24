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

package org.openvpms.archetype.rules.product.io;

import org.apache.commons.lang.ObjectUtils;
import org.junit.Before;
import org.junit.Test;
import org.openvpms.archetype.rules.practice.PracticeArchetypes;
import org.openvpms.archetype.rules.product.ProductArchetypes;
import org.openvpms.archetype.rules.product.ProductPriceRules;
import org.openvpms.archetype.test.ArchetypeServiceTest;
import org.openvpms.component.business.domain.im.party.Party;
import org.openvpms.component.business.domain.im.product.Product;
import org.openvpms.component.business.domain.im.product.ProductPrice;
import org.openvpms.component.business.service.archetype.helper.IMObjectBean;
import org.openvpms.component.business.service.lookup.ILookupService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openvpms.archetype.rules.product.ProductPriceTestHelper.createFixedPrice;
import static org.openvpms.archetype.rules.product.ProductPriceTestHelper.createUnitPrice;
import static org.openvpms.archetype.test.TestHelper.getDate;

/**
 * Tests the {@link ProductImporter}.
 *
 * @author Tim Anderson
 */
public class ProductImporterTestCase extends ArchetypeServiceTest {

    /**
     * The lookup service.
     */
    @Autowired
    private ILookupService lookups;

    /**
     * The price rules.
     */
    private ProductPriceRules rules;

    /**
     * The first test product.
     */
    private Product product1;

    /**
     * The second test product.
     */
    private Product product2;

    /**
     * The product1 fixed price.
     */
    private ProductPrice fixed1;

    /**
     * The product1 unit price.
     */
    private ProductPrice unit1;

    /**
     * The product2 fixed price.
     */
    private ProductPrice fixed2;

    /**
     * The product2 unit price.
     */
    private ProductPrice unit2;

    /**
     * The practice.
     */
    private Party practice;

    /**
     * The product importer.
     */
    private ProductImporter importer;

    /**
     * Sets up the test case.
     */
    @Before
    public void setUp() {
        rules = new ProductPriceRules(getArchetypeService(), lookups);

        importer = new ProductImporter(getArchetypeService(), rules);
        practice = (Party) create(PracticeArchetypes.PRACTICE);
        product1 = ProductIOTestHelper.createProduct("Product 1", "P1");
        product2 = ProductIOTestHelper.createProduct("Product 2", "P2");

        fixed1 = createFixedPrice("1.0", "0.5", "100", "2013-02-01", "2013-04-01", true);
        unit1 = createUnitPrice("1.92", "1.2", "60", "2013-02-02", "2013-04-02");
        product1.addProductPrice(fixed1);
        product1.addProductPrice(unit1);

        fixed2 = createFixedPrice("1.08", "0.6", "80", "2013-04-02", "2013-06-01", true);
        unit2 = createUnitPrice("2.55", "1.5", "70", "2013-04-03", "2013-06-02");
        product2.addProductPrice(fixed2);
        product2.addProductPrice(unit2);
        save(product1, product2);
    }

    /**
     * Verifies that when an import is run on data that has no changes, no product is updated.
     */
    @Test
    public void testImportNoChanges() {
        ProductData data1 = createProduct(product1);
        ProductData data2 = createProduct(product2);

        importProducts(data1, data2);

        product1 = get(product1);
        product2 = get(product2);

        assertEquals(2, product1.getProductPrices().size());
        assertEquals(2, product2.getProductPrices().size());

        checkPrice(product1, fixed1);
        checkPrice(product1, unit1);

        checkPrice(product2, fixed2);
        checkPrice(product2, unit2);
    }

    /**
     * Verifies that an existing price will be updated if has the same dates.
     */
    @Test
    public void testUpdateExisting() {
        ProductData data = createProduct(product1);
        BigDecimal fixedPrice = new BigDecimal("2.0");
        BigDecimal fixedCost = new BigDecimal("1.0");
        BigDecimal markup = new BigDecimal("100");
        BigDecimal unitPrice = new BigDecimal("1.0");
        BigDecimal unitCost = new BigDecimal("0.5");

        PriceData fixed = data.getFixedPrices().get(0);
        fixed.setPrice(fixedPrice);
        fixed.setCost(fixedCost);

        PriceData unit = data.getUnitPrices().get(0);
        unit.setPrice(unitPrice);
        unit.setCost(unitCost);

        importProducts(data);

        product1 = get(product1);
        assertEquals(2, product1.getProductPrices().size());

        checkPrice(product1, ProductArchetypes.FIXED_PRICE, fixedPrice, fixedCost, markup, fixed.getFrom(),
                   fixed.getTo());
        checkPrice(product1, ProductArchetypes.UNIT_PRICE, unitPrice, unitCost, markup, unit.getFrom(), unit.getTo());
    }

    /**
     * Verifies that a new price closes modifies the end time of an existing price, to avoid overlaps.
     */
    @Test
    public void testCreateNewUndatedPrice() {
        ProductData data = createProduct(product1);
        BigDecimal fixedPrice = new BigDecimal("2.0");
        BigDecimal fixedCost = new BigDecimal("1.0");
        BigDecimal markup = new BigDecimal("100");
        BigDecimal unitPrice = new BigDecimal("1.0");
        BigDecimal unitCost = new BigDecimal("0.5");

        data.addFixedPrice(fixedPrice, fixedCost, null, null);
        data.addUnitPrice(unitPrice, unitCost, null, null);

        importProducts(data);

        product1 = get(product1);
        assertEquals(4, product1.getProductPrices().size());

        checkPrice(product1, fixed1);
        checkPrice(product1, unit1);
        checkPrice(product1, ProductArchetypes.FIXED_PRICE, fixedPrice, fixedCost, markup, getDate("2013-04-02"), null);
        checkPrice(product1, ProductArchetypes.UNIT_PRICE, unitPrice, unitCost, markup, getDate("2013-04-03"), null);
    }

    /**
     * Verifies that a product contains the expected price.
     *
     * @param product  the product
     * @param expected the expected price
     */
    private void checkPrice(Product product, ProductPrice expected) {
        IMObjectBean bean = new IMObjectBean(expected);
        BigDecimal expectedPrice = expected.getPrice();
        BigDecimal expectedCost = bean.getBigDecimal("cost");
        BigDecimal expectedMarkup = bean.getBigDecimal("markup");
        Date expectedFrom = expected.getFromDate();
        Date expectedTo = expected.getToDate();

        String shortName = expected.getArchetypeId().getShortName();
        checkPrice(product, shortName, expectedPrice, expectedCost, expectedMarkup, expectedFrom, expectedTo);
    }

    /**
     * Verifies that a product contains the expected price.
     *
     * @param product        the product
     * @param shortName      the price archetype short name
     * @param expectedPrice  the expected price
     * @param expectedCost   the expected cost
     * @param expectedMarkup the expected markup
     * @param expectedFrom   the expected price start date
     * @param expectedTo     the expected price end date
     */
    private void checkPrice(Product product, String shortName, BigDecimal expectedPrice, BigDecimal expectedCost,
                            BigDecimal expectedMarkup, Date expectedFrom, Date expectedTo) {
        boolean found = false;
        for (ProductPrice price : product.getProductPrices()) {
            IMObjectBean priceBean = new IMObjectBean(price);
            if (price.getArchetypeId().getShortName().equals(shortName)
                && price.getPrice().compareTo(expectedPrice) == 0
                && priceBean.getBigDecimal("cost").compareTo(expectedCost) == 0
                && priceBean.getBigDecimal("markup").compareTo(expectedMarkup) == 0
                && ObjectUtils.equals(expectedFrom, price.getFromDate())
                && ObjectUtils.equals(expectedTo, price.getToDate())) {
                found = true;
                break;
            }
        }
        assertTrue("Failed to find price", found);
    }

    /**
     * Imports products.
     *
     * @param data the product data to import
     */
    private void importProducts(ProductData... data) {
        List<ProductData> products = Arrays.asList(data);
        importer.run(products, practice);
    }

    /**
     * Creates a new {@link ProductData} from a {@link Product}.
     *
     * @param product the product
     * @return the corresponding product data
     */
    private ProductData createProduct(Product product) {
        IMObjectBean bean = new IMObjectBean(product);
        ProductData result = new ProductData(product.getId(), product.getName(), bean.getString("printedName"), 1);
        result.setReference(product.getObjectReference());
        for (ProductPrice price : rules.getProductPrices(product, ProductArchetypes.FIXED_PRICE)) {
            IMObjectBean priceBean = new IMObjectBean(price);
            result.addFixedPrice(price.getPrice(), priceBean.getBigDecimal("cost"), price.getFromDate(),
                                 price.getToDate());
        }
        for (ProductPrice price : rules.getProductPrices(product, ProductArchetypes.UNIT_PRICE)) {
            IMObjectBean priceBean = new IMObjectBean(price);
            result.addUnitPrice(price.getPrice(), priceBean.getBigDecimal("cost"), price.getFromDate(),
                                price.getToDate());
        }
        return result;
    }

}
