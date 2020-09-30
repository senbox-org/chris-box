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

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link ComputeSurfaceReflectancesOp}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ComputeSurfaceReflectancesOpTest {
    @Test
    public void testEndmemberTableIntegrity() {
        final InputStream is = ComputeSurfaceReflectancesOpTest.class.getResourceAsStream("endmembers.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 811;
        final ArrayList<Double> x = new ArrayList<>(rowCount);
        final ArrayList<Double> y = new ArrayList<>(rowCount);
        final ArrayList<Double> z = new ArrayList<>(rowCount);

        try {
            while (scanner.hasNext()) {
                x.add(scanner.nextDouble());
                y.add(scanner.nextDouble());
                z.add(scanner.nextDouble());
            }
        } finally {
            scanner.close();
        }

        Assert.assertEquals(rowCount, x.size());
        Assert.assertEquals(rowCount, y.size());
        Assert.assertEquals(rowCount, z.size());

// code for writing the img file
//        final FileImageOutputStream ios = new FileImageOutputStream(new File("endmembers.img"));
//        ios.writeInt(rowCount);
//        for (final Double value : x) {
//            ios.writeDouble(value * 1000.0);
//        }
//        for (final Double value : y) {
//            ios.writeDouble(value);
//        }
//        for (final Double value : z) {
//            ios.writeDouble(value);
//        }
//        ios.close();

        final double[][] table = ComputeSurfaceReflectancesOp.readEndmemberTable();

        Assert.assertEquals(rowCount, table[0].length);
        Assert.assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            Assert.assertEquals(x.get(i) * 1000.0, table[0][i], 0.0);
            Assert.assertEquals(y.get(i), table[1][i], 0.0);
            Assert.assertEquals(z.get(i), table[2][i], 0.0);
        }
    }
}
