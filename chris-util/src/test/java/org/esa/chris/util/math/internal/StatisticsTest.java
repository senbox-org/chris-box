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
package org.esa.chris.util.math.internal;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test methods for class {@link Statistics}.
 *
 * @author Ralf Quast
 * @version $Revision: 2530 $ $Date: 2008-07-09 13:10:39 +0200 (Wed, 09 Jul 2008) $
 * @since BEAM 4.2
 */
public class StatisticsTest {

    private static final double DSQRT_2 = Math.sqrt(2.0);

    @Test
    public void testCountFloat() {
        try {
            Statistics.count((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertEquals(0, Statistics.count(new float[]{}));
        Assert.assertEquals(0, Statistics.count(new float[]{Float.NaN}));
        Assert.assertEquals(0, Statistics.count(new float[]{Float.POSITIVE_INFINITY}));
        Assert.assertEquals(0, Statistics.count(new float[]{Float.NEGATIVE_INFINITY}));

        Assert.assertEquals(1, Statistics.count(new float[]{1.0f}));
        Assert.assertEquals(3, Statistics.count(new float[]{1.0f, 2.0f, 3.0f}));

        Assert.assertEquals(2, Statistics.count(new float[]{1.0f, Float.NaN, 3.0f}));
        Assert.assertEquals(2, Statistics.count(new float[]{1.0f, Float.POSITIVE_INFINITY, 3.0f}));
        Assert.assertEquals(2, Statistics.count(new float[]{1.0f, Float.NEGATIVE_INFINITY, 3.0f}));
    }

    @Test
    public void testCountDouble() {
        try {
            Statistics.count((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertEquals(0, Statistics.count(new double[]{}));
        Assert.assertEquals(0, Statistics.count(new double[]{Float.NaN}));
        Assert.assertEquals(0, Statistics.count(new double[]{Float.POSITIVE_INFINITY}));
        Assert.assertEquals(0, Statistics.count(new double[]{Float.NEGATIVE_INFINITY}));

        Assert.assertEquals(1, Statistics.count(new double[]{1.0}));
        Assert.assertEquals(3, Statistics.count(new double[]{1.0, 2.0, 3.0}));

        Assert.assertEquals(2, Statistics.count(new double[]{1.0, Float.NaN, 3.0}));
        Assert.assertEquals(2, Statistics.count(new double[]{1.0, Float.POSITIVE_INFINITY, 3.0}));
        Assert.assertEquals(2, Statistics.count(new double[]{1.0, Float.NEGATIVE_INFINITY, 3.0}));
    }

    @Test
    public void testMeanFloat() {
        try {
            Statistics.mean((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.mean(new float[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new float[]{Float.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new float[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new float[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0f, Statistics.mean(new float[]{1.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.mean(new float[]{1.0f, 2.0f, 3.0f}), 0.0f);

        Assert.assertEquals(2.0f, Statistics.mean(new float[]{1.0f, Float.NaN, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.mean(new float[]{1.0f, Float.POSITIVE_INFINITY, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.mean(new float[]{1.0f, Float.NEGATIVE_INFINITY, 3.0f}), 0.0f);
    }

    @Test
    public void testMeanDouble() {
        try {
            Statistics.mean((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.mean(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new double[]{Double.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new double[]{Double.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.mean(new double[]{Double.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0, Statistics.mean(new double[]{1.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.mean(new double[]{1.0, 2.0, 3.0}), 0.0);

        Assert.assertEquals(2.0, Statistics.mean(new double[]{1.0, Double.NaN, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.mean(new double[]{1.0, Double.POSITIVE_INFINITY, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.mean(new double[]{1.0, Double.NEGATIVE_INFINITY, 3.0}), 0.0);
    }

    @Test
    public void testCoefficientOfVariationFloat() {
        Assert.assertEquals(0.0f, Statistics.cv(new float[]{1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(0.0f, Statistics.cv(new float[]{1.0f, 1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(0.5f, Statistics.cv(new float[]{1.0f, 2.0f, 3.0f}), 0.0f);

        final float cv = Statistics.cv(new float[]{1.0f, Float.NaN, 3.0f});
        Assert.assertEquals((float) (DSQRT_2 / 2.0), cv, 0.0f);
    }

    @Test
    public void testCoefficientOfVariationDouble() {
        Assert.assertEquals(0.0, Statistics.cv(new double[]{1.0, 1.0}), 0.0);
        Assert.assertEquals(0.0, Statistics.cv(new double[]{1.0, 1.0, 1.0}), 0.0);
        Assert.assertEquals(0.5, Statistics.cv(new double[]{1.0, 2.0, 3.0}), 0.0);

        final double cv = Statistics.cv(new double[]{1.0, Double.NaN, 3.0});
        Assert.assertEquals(DSQRT_2 / 2.0, cv, 0.0);
    }

    @Test
    public void testStandardDeviationFloat() {
        Assert.assertEquals(0.0f, Statistics.sdev(new float[]{1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(0.0f, Statistics.sdev(new float[]{1.0f, 1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.sdev(new float[]{1.0f, 2.0f, 3.0f}), 0.0f);

        final float stdev = Statistics.sdev(new float[]{1.0f, Float.NaN, 3.0f});
        Assert.assertEquals((float) DSQRT_2, stdev, 0.0f);
    }

    @Test
    public void testStandardDeviationDouble() {
        Assert.assertEquals(0.0, Statistics.sdev(new double[]{1.0, 1.0}), 0.0);
        Assert.assertEquals(0.0, Statistics.sdev(new double[]{1.0, 1.0, 1.0}), 0.0);
        Assert.assertEquals(1.0, Statistics.sdev(new double[]{1.0, 2.0, 3.0}), 0.0);

        final double stdev = Statistics.sdev(new double[]{1.0, Double.NaN, 3.0});
        Assert.assertEquals(DSQRT_2, stdev, 0.0);
    }

    @Test
    public void testVarianceFloat() {
        try {
            Statistics.variance((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{Float.NaN})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{Float.NEGATIVE_INFINITY})));

        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{1.0f})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{1.0f, Float.NaN})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{1.0f, Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Float.isNaN(Statistics.variance(new float[]{1.0f, Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(0.0f, Statistics.variance(new float[]{1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(0.0f, Statistics.variance(new float[]{1.0f, 1.0f, 1.0f}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.variance(new float[]{1.0f, 2.0f, 3.0f}), 0.0f);

        Assert.assertEquals(2.0f, Statistics.variance(new float[]{1.0f, Float.NaN, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.variance(new float[]{1.0f, Float.POSITIVE_INFINITY, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.variance(new float[]{1.0f, Float.NEGATIVE_INFINITY, 3.0f}), 0.0f);
    }

    @Test
    public void testVarianceDouble() {
        try {
            Statistics.variance((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{Double.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{Double.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{Double.NEGATIVE_INFINITY})));

        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{1.0})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{1.0, Double.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{1.0, Double.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.variance(new double[]{1.0, Double.NEGATIVE_INFINITY})));

        Assert.assertEquals(0.0, Statistics.variance(new double[]{1.0, 1.0}), 0.0);
        Assert.assertEquals(0.0, Statistics.variance(new double[]{1.0, 1.0, 1.0}), 0.0);
        Assert.assertEquals(1.0, Statistics.variance(new double[]{1.0, 2.0, 3.0}), 0.0);

        Assert.assertEquals(2.0, Statistics.variance(new double[]{1.0, Double.NaN, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.variance(new double[]{1.0, Double.POSITIVE_INFINITY, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.variance(new double[]{1.0, Double.NEGATIVE_INFINITY, 3.0}), 0.0);
    }

    @Test
    public void testMinFloat() {
        try {
            Statistics.min((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Float.isNaN(Statistics.min(new float[]{})));
        Assert.assertTrue(Float.isNaN(Statistics.min(new float[]{Float.NaN})));
        Assert.assertTrue(Float.isNaN(Statistics.min(new float[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Float.isNaN(Statistics.min(new float[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0f, Statistics.min(new float[]{1.0f}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.min(new float[]{1.0f, Float.NaN}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.min(new float[]{1.0f, Float.POSITIVE_INFINITY}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.min(new float[]{1.0f, Float.NEGATIVE_INFINITY}), 0.0f);

        Assert.assertEquals(1.0f, Statistics.min(new float[]{1.0f, 2.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.min(new float[]{3.0f, 2.0f}), 0.0f);
    }

    @Test
    public void testMinDouble() {
        try {
            Statistics.min((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.min(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.min(new double[]{Float.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.min(new double[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.min(new double[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0, Statistics.min(new double[]{1.0}), 0.0);
        Assert.assertEquals(1.0, Statistics.min(new double[]{1.0, Double.NaN}), 0.0);
        Assert.assertEquals(1.0, Statistics.min(new double[]{1.0, Double.POSITIVE_INFINITY}), 0.0);
        Assert.assertEquals(1.0, Statistics.min(new double[]{1.0, Double.NEGATIVE_INFINITY}), 0.0);

        Assert.assertEquals(1.0, Statistics.min(new double[]{1.0, 2.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.min(new double[]{3.0, 2.0}), 0.0);
    }

    @Test
    public void testMaxFloat() {
        try {
            Statistics.max((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Float.isNaN(Statistics.max(new float[]{})));
        Assert.assertTrue(Float.isNaN(Statistics.max(new float[]{Float.NaN})));
        Assert.assertTrue(Float.isNaN(Statistics.max(new float[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Float.isNaN(Statistics.max(new float[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0f, Statistics.max(new float[]{1.0f}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.max(new float[]{1.0f, Float.NaN}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.max(new float[]{1.0f, Float.POSITIVE_INFINITY}), 0.0f);
        Assert.assertEquals(1.0f, Statistics.max(new float[]{1.0f, Float.NEGATIVE_INFINITY}), 0.0f);

        Assert.assertEquals(2.0f, Statistics.max(new float[]{1.0f, 2.0f}), 0.0f);
        Assert.assertEquals(3.0f, Statistics.max(new float[]{3.0f, 2.0f}), 0.0f);
    }

    @Test
    public void testMaxDouble() {
        try {
            Statistics.max((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.max(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.max(new double[]{Float.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.max(new double[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.max(new double[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0, Statistics.max(new double[]{1.0}), 0.0);
        Assert.assertEquals(1.0, Statistics.max(new double[]{1.0, Double.NaN}), 0.0);
        Assert.assertEquals(1.0, Statistics.max(new double[]{1.0, Double.POSITIVE_INFINITY}), 0.0);
        Assert.assertEquals(1.0, Statistics.max(new double[]{1.0, Double.NEGATIVE_INFINITY}), 0.0);

        Assert.assertEquals(2.0, Statistics.max(new double[]{1.0, 2.0}), 0.0);
        Assert.assertEquals(3.0, Statistics.max(new double[]{3.0, 2.0}), 0.0);
    }

    @Test
    public void testMedianFloat() {
        try {
            Statistics.median((float[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Float.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Float.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Float.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0f, Statistics.median(new double[]{1.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.median(new double[]{1.0f, 2.0f, 3.0f}), 0.0f);
        Assert.assertEquals(2.5f, Statistics.median(new double[]{1.0f, 2.0f, 3.0f, 4.0f}), 0.0f);

        Assert.assertEquals(2.0f, Statistics.median(new double[]{1.0f, Double.NaN, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.median(new double[]{1.0f, Double.POSITIVE_INFINITY, 3.0f}), 0.0f);
        Assert.assertEquals(2.0f, Statistics.median(new double[]{1.0f, Double.NEGATIVE_INFINITY, 3.0f}), 0.0f);
    }

    @Test
    public void testMedianDouble() {
        try {
            Statistics.median((double[]) null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }

        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Double.NaN})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Double.POSITIVE_INFINITY})));
        Assert.assertTrue(Double.isNaN(Statistics.median(new double[]{Double.NEGATIVE_INFINITY})));

        Assert.assertEquals(1.0, Statistics.median(new double[]{1.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.median(new double[]{1.0, 2.0, 3.0}), 0.0);
        Assert.assertEquals(2.5, Statistics.median(new double[]{1.0, 2.0, 3.0, 4.0}), 0.0);

        Assert.assertEquals(2.0, Statistics.median(new double[]{1.0, Double.NaN, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.median(new double[]{1.0, Double.POSITIVE_INFINITY, 3.0}), 0.0);
        Assert.assertEquals(2.0, Statistics.median(new double[]{1.0, Double.NEGATIVE_INFINITY, 3.0}), 0.0);
    }
}
