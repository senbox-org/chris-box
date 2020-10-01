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
package org.esa.chris.noise.ui;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductFilter;

/**
 * Filters CHRIS/Proba products suitable for the noise correction.
 *
 * @author Ralf Quast
 */
class NoiseReductionProductFilter implements ProductFilter {

    @Override
    public boolean accept(Product product) {
        return product != null && product.getProductType().matches("CHRIS_M[12345][0A]?");
    }
}
