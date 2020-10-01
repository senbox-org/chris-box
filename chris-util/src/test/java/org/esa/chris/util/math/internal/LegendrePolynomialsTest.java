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
 * Tests for class {@link LegendrePolynomials}.
 *
 * @author Ralf Quast
 */
public class LegendrePolynomialsTest {

    @Test
    public void testCalculationForNullArray() {
        try {
            new LegendrePolynomials().calculate(0.5, null);
            Assert.fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void testCalculateForEmptyArray() {
        new LegendrePolynomials().calculate(0.5, new double[0]);
    }

    @Test
    public void testCalculate() {
        double[] y = new double[5];
        new LegendrePolynomials().calculate(0.5, y);

        Assert.assertEquals(1.0, y[0], 0.0);
        Assert.assertEquals(0.5, y[1], 0.0);
        Assert.assertEquals(-0.125, y[2], 0.0);
        Assert.assertEquals(-0.4375, y[3], 0.0);
        Assert.assertEquals(-0.2890625, y[4], 0.0);
    }

}
