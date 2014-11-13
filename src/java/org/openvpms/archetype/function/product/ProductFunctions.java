/**
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
 * Copyright 2014 (C) OpenVPMS Ltd. All Rights Reserved.
 */

package org.openvpms.archetype.function.product;

import java.util.Date;
import org.openvpms.archetype.rules.product.ProductRules;
import org.openvpms.component.business.domain.im.common.Entity;
import org.openvpms.component.business.service.archetype.IArchetypeService;


/**
 *
 * @author benjamincharlton
 */
public class ProductFunctions {
  
    /**
     * The archetype service.
     */
    private final IArchetypeService service;
    
    private final ProductRules productRules;
    /**
     * Constructs a @link ProductFunctions
     * 
     * @param service  IArchetypeService
     * @param productRules  Product Rules
     */
    public ProductFunctions(IArchetypeService service, ProductRules productRules) {
        this.service = service;
        this.productRules = productRules;
    }
    /**
     * Returns the most recent product barcode as a String
     * @param product
     * @return String
     */
    public String getBarcode(Entity product){
        return productRules.getBarcode(product);
    }
    /**
     * Returns all the barcodes as a comma delimited string
     * @param product
     * @return String
     */
    public String getBarcodes(Entity product){
        return productRules.getBarcodes(product);
    }
    /**
     * Returns the batch expiry date as a date
     * @param batch A batch entity.
     * @return java.util.date
     */
    public Date getBatchExpiry(Entity batch){
        return productRules.getBatchExpiry(batch);
    }
}
