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

package org.esa.chris.cloud.operators;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link ExtractFeaturesOp}.
 *
 * @author Ralf Quast
 */
public class ExtractFeaturesOpTest {

    @Test
    public void testTransmittanceTableIntegrity() throws IOException {
        assertTransmittanceTableIntegrity();
    }

    private static void assertTransmittanceTableIntegrity() throws IOException {
        final InputStream is = ExtractFeaturesOpTest.class.getResourceAsStream("nir-transmittance.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 4334;
        final ArrayList<Double> x = new ArrayList<>(rowCount);
        final ArrayList<Double> y = new ArrayList<>(rowCount);

        try {
            while (scanner.hasNext()) {
                x.add(scanner.nextDouble());
                y.add(scanner.nextDouble());
            }
        } finally {
            scanner.close();
        }

        Assert.assertEquals(rowCount, x.size());
        Assert.assertEquals(rowCount, y.size());

//        final FileImageOutputStream ios = new FileImageOutputStream(new File("nir-transmittance.img"));
//        ios.writeInt(rowCount);
//        for (final Double value : x) {
//            ios.writeDouble(value);
//        }
//        for (final Double value : y) {
//            ios.writeDouble(value);
//        }
//        ios.close();

        final double[][] table = ExtractFeaturesOp.readTransmittanceTable();

        Assert.assertEquals(rowCount, table[0].length);
        Assert.assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            Assert.assertEquals(x.get(i), new Double(table[0][i]));
            Assert.assertEquals(y.get(i), new Double(table[1][i]));
        }
    }
}
