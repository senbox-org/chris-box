/*
 * Copyright (C) 2010-2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.chris.cloud.operators.lut;

import org.esa.chris.cloud.operators.lut.ClassOpImage;
import org.esa.snap.cluster.Distribution;
import org.esa.snap.cluster.IndexFilter;
import org.esa.snap.cluster.ProbabilityCalculator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Assert;
import org.junit.Test;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 * Tests for class {@link ClassOpImage}.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class ClassOpImageTest {

    private static final IndexFilter NO_FILTERING = index -> true;

    @Test
    public void testComputation() {
        final Product product = createTestProduct();
        final Distribution[] distributions = new Distribution[4];

        distributions[0] = new StandardMultinormalDistribution(new double[]{10.0, 10.0, 10.0, 10.0});
        distributions[1] = new StandardMultinormalDistribution(new double[]{20.0, 20.0, 20.0, 20.0});
        distributions[2] = new StandardMultinormalDistribution(new double[]{30.0, 30.0, 30.0, 30.0});
        distributions[3] = new StandardMultinormalDistribution(new double[]{40.0, 40.0, 40.0, 40.0});

        final double[] priors = {1.0, 1.0, 1.0, 1.0};
        final ProbabilityCalculator calculator = new ProbabilityCalculator(distributions, priors);

        final RenderedImage image = ClassOpImage.createImage(product.getBands(), calculator, NO_FILTERING, 4);
        final Raster data = image.getData();

        Assert.assertEquals(0, data.getSample(0, 0, 0));
        Assert.assertEquals(3, data.getSample(1, 0, 0));
        Assert.assertEquals(2, data.getSample(0, 1, 0));
        Assert.assertEquals(1, data.getSample(1, 1, 0));
    }

    private static Product createTestProduct() {
        final Product product = new Product("Features", "Features", 2, 2);

        addSourceBand(product, "feature_0", new short[]{101, 401, 301, 201});
        addSourceBand(product, "feature_1", new short[]{102, 402, 302, 202});
        addSourceBand(product, "feature_2", new short[]{103, 403, 303, 203});
        addSourceBand(product, "feature_3", new short[]{104, 404, 304, 204});

        return product;
    }

    private static void addSourceBand(Product product, String name, short[] samples) {
        final Band band = product.addBand(name, ProductData.TYPE_INT16);
        band.setScalingFactor(1.0 / 10.0);

        band.setSynthetic(true);
        band.setRasterData(ProductData.createInstance(samples));
    }

    private static class StandardMultinormalDistribution implements Distribution {
        private final double[] mean;

        public StandardMultinormalDistribution(double[] mean) {
            this.mean = mean;
        }

        @Override
        public final double probabilityDensity(double[] y) {
            return Math.exp(logProbabilityDensity(y));
        }

        @Override
        public final double logProbabilityDensity(double[] y) {
            if (y.length != mean.length) {
                throw new IllegalArgumentException("y.length != mean.length");
            }

            return -0.5 * squaredDistance(y);
        }

        private double squaredDistance(double[] y) {
            double u = 0.0;

            for (int i = 0; i < mean.length; ++i) {
                u += ((y[i] - mean[i]) * (y[i] - mean[i]));
            }

            return u;
        }
    }
}
