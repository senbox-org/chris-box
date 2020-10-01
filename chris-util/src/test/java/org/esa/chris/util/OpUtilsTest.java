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

package org.esa.chris.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Tests for class {@link OpUtils}.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since CHRIS-BOX 1.0
 */
public class OpUtilsTest {

    @Test
    public void testThuillierTableIntegrity() {
        final InputStream is = OpUtilsTest.class.getResourceAsStream("thuillier.txt");

        final Scanner scanner = new Scanner(is);
        scanner.useLocale(Locale.ENGLISH);

        final int rowCount = 8191;
        final List<Double> x = new ArrayList<>(rowCount);
        final List<Double> y = new ArrayList<>(rowCount);

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

//      was used to create the thuillier.img file
//      final FileImageOutputStream ios = new FileImageOutputStream(new File("thuillier.img"));
//      ios.writeInt(rowCount);
//      for (final Double value : x) {
//          ios.writeDouble(value);
//      }
//      for (final Double value : y) {
//          ios.writeDouble(value);
//      }
//      ios.close();

        final double[][] table = OpUtils.readThuillierTable();

        Assert.assertEquals(rowCount, table[0].length);
        Assert.assertEquals(rowCount, table[1].length);

        for (int i = 0; i < rowCount; ++i) {
            Assert.assertEquals(x.get(i), (Double)table[0][i]);
            Assert.assertEquals(y.get(i), (Double)table[1][i]);
        }
    }
}
