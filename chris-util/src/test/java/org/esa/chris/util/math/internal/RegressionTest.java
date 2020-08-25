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
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class RegressionTest {

    @Test
    public void testRegression() {
        final UnivariateFunctionSequence legendrePolynomials = new LegendrePolynomials();
        final double[][] matrix = new double[3][5];

        legendrePolynomials.calculate(new double[]{0.0, 1.0, 2.0, 3.0, 4.0}, matrix);
        final double[] y = {0.0, 3.0, 12.0, 27.0, 48.0}; // parabola
        double[] z = new Regression(matrix).fit(y);

        Assert.assertEquals(y[0], z[0], 1.0E-10);
        Assert.assertEquals(y[1], z[1], 1.0E-10);
        Assert.assertEquals(y[2], z[2], 1.0E-10);
        Assert.assertEquals(y[3], z[3], 1.0E-10);
        Assert.assertEquals(y[4], z[4], 1.0E-10);
    }
}
