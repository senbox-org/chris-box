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

package org.esa.chris.geocorr.operators;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class CoordinateConverterTest {

    @Test
    public void ecefToEci() {
        // ECEF coordinates of the Hamburger Sternwarte (WGS-84)
        // lon = 10.2414° E
        // lat = 53.4800° N
        // alt = 40.0 m
        final double ecefX = 3743300.458313003;
        final double ecefY = 676318.7711106260;
        final double ecefZ = 5102545.269331731;
        final double[] ecef = new double[]{ecefX, ecefY, ecefZ};

        final double[] eci = CoordinateConverter.ecefToEci(Math.PI / 2.0, ecef, ecef.clone());
        final double eciX = eci[0];
        final double eciY = eci[1];
        final double eciZ = eci[2];

        assertEquals(-ecefY, eciX, 5.0E-10);
        assertEquals(ecefX, eciY, 5.0E-10);
        assertEquals(ecefZ, eciZ, 0.0);
    }

    @Test
    public void eciToEcef() {
        final double eciX = -676318.7711106260;
        final double eciY = 3743300.458313003;
        final double eciZ = 5102545.269331731;
        final double[] eci = new double[]{eciX, eciY, eciZ};

        final double[] ecef = CoordinateConverter.eciToEcef(Math.PI / 2.0, eci, eci.clone());
        final double ecefX = ecef[0];
        final double ecefY = ecef[1];
        final double ecefZ = ecef[2];

        assertEquals(eciY, ecefX, 5.0E-10);
        assertEquals(-eciX, ecefY, 5.0E-10);
        assertEquals(eciZ, ecefZ, 0.0);
    }
}
