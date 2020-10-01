/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.chris.ac.ui;

import org.esa.chris.util.OpUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductFilter;

/**
 * Filters CHRIS/Proba products suitable for the atmospheric correction.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-BOX 1.0
 */
class AtmosphericCorrectionProductFilter implements ProductFilter {
    @Override
    public boolean accept(Product product) {
        if (product == null || !product.getProductType().matches("CHRIS_M[12345][0A]?_NR")) {
            return false;
        }

        final Band[] radianceBands = OpUtils.findBands(product, "radiance_");
        final Band[] maskBands = OpUtils.findBands(product, "mask_");

        return radianceBands.length >= 18 && radianceBands.length == maskBands.length;
    }
}
