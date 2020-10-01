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

package org.esa.chris.ac.operators;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link WaterMaskOpImage}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since CHRIS-BOX 1.0
 */
public class WaterMaskOpImageTest {

    private static final int W = 2;
    private static final int H = 2;

    @Test
    public void testImageComputation() {
        final RenderedImage image = createTestImage();
        final Raster raster = image.getData();

        Assert.assertEquals(0, raster.getSample(0, 0, 0)); // red < NIR
        Assert.assertEquals(1, raster.getSample(1, 0, 0)); // red > NIR
        Assert.assertEquals(0, raster.getSample(0, 1, 0)); // red > NIR - but NIR too big
        Assert.assertEquals(0, raster.getSample(1, 1, 0)); // red > NIR - but NIR too small
    }

    private static RenderedImage createTestImage() {
        final Product product = new Product("test", "test", W, H);

        final Band redBand = addBand(product, "red", new int[]{20, 40, 180, 20});
        final Band nirBand = addBand(product, "nir", new int[]{40, 20, 160, 10});

        return WaterMaskOpImage.createImage(redBand, nirBand, 0.001, 0.001);
    }

    private static Band addBand(Product product, String name, int[] values) {
        final Band band = product.addBand(name, ProductData.TYPE_INT32);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(values));

        return band;
    }
}
