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

package org.esa.chris.geocorr.operators;

import static java.lang.Math.*;

/**
 * Coordinate converter.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class CoordinateConverter {

    /**
     * Angular rotational velocity of the Earth (rad s-1)
     */
    private static final double WE = 7.292115854788046E-5;

    /**
     * Flattening of WGS-84 ellipsoid.
     */
    private static final double WGS84_F = 1.0 / 298.257223563;
    /**
     * Eccentricity squared.
     */
    private static final double WGS84_E = WGS84_F * (2.0 - WGS84_F);
    /**
     * Major radius of WGS-84 ellipsoid (km).
     */
    private static final double WGS84_A = 6378.137;
    /**
     * Minor radius of WGS-84 ellipsoid (km).
     */
    private static final double WGS84_B = WGS84_A * (1.0 - WGS84_F);

    private static final double ONE_THIRD = 1.0 / 3.0;
    private static final double FOUR_THIRD = 4.0 / 3.0;

    private final double c;
    private final double s;

    public static double[] ecefToEci(double gst, double[] ecef, double[] eci) {
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (eci == null) {
            throw new IllegalArgumentException("eci == null");
        }
        if (ecef.length < 2) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length < 2) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length > 6) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length > 6) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (eci.length != ecef.length) {
            throw new IllegalArgumentException("eci.length != ecef.length");
        }

        final double c = cos(gst);
        final double s = sin(gst);

        return ecefToEci(c, s, ecef, eci);
    }

    public static double[] ecefToWgs(double x, double y, double z, double[] wgs) {
        if (wgs == null) {
            throw new IllegalArgumentException("wgs == null");
        }
        if (wgs.length != 3) {
            throw new IllegalArgumentException("wgs.length != 3");
        }
        final double b = WGS84_B * signum(z);
        final double r = sqrt(x * x + y * y);
        final double s = WGS84_A * WGS84_A - b * b;
        final double e = (b * z - s) / (WGS84_A * r);
        final double f = (b * z + s) / (WGS84_A * r);
        final double p = FOUR_THIRD * (e * f + 1.0);
        final double q = 2.0 * (e * e - f * f);
        final double d = sqrt(p * p * p + q * q);
        final double v = pow(d - q, ONE_THIRD) - pow(d + q, ONE_THIRD);
        final double g = (sqrt(e * e + v) + e) / 2.0;
        final double t = sqrt(g * g + (f - v * g) / (2.0 * g - e)) - g;

        double phi = atan2(WGS84_A * (1.0 - t * t), 2.0 * b * t);
        double lam = atan2(y, x);
        double alt = (r - WGS84_A * t) * cos(phi) + (z - b) * sin(phi);

        if (x == 0.0 && y == 0.0 && z != 0.0) {
            phi = Math.PI / 2.0 * signum(z);
            lam = 0.0;
            alt = Math.abs(z - b);
        }
        if (z == 0.0) {
            phi = 0.0;
            alt = r - WGS84_A;
        }
        double lon = toDegrees(lam);
        if (lon > 180.0) {
            lon -= 360.0;
        }
        double lat = toDegrees(phi);
        if (lat > 90.0) {
            lat -= 180.0;
        }

        wgs[0] = lon;
        wgs[1] = lat;
        wgs[2] = alt;

        return wgs;
    }

    public static double[] eciToEcef(double gst, double[] eci, double[] ecef) {
        if (eci == null) {
            throw new IllegalArgumentException("eci == null");
        }
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (eci.length < 2) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length < 2) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length > 6) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length > 6) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (ecef.length != eci.length) {
            throw new IllegalArgumentException("ecef.length != eci.length");
        }

        final double c = cos(gst);
        final double s = sin(gst);

        return eciToEcef(c, s, eci, ecef);
    }

    public static double[] wgsToEcef(double lon, double lat, double alt, double[] ecef) {
        if (Math.abs(lat) > 90.0) {
            throw new IllegalArgumentException("|lat| > 90.0");
        }
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (ecef.length != 3) {
            throw new IllegalArgumentException("ecef.length != 3");
        }

        final double u = Math.toRadians(lon);
        final double v = Math.toRadians(lat);

        final double cu = cos(u);
        final double su = sin(u);
        final double cv = cos(v);
        final double sv = sin(v);

        final double a = WGS84_A / sqrt(1.0 - WGS84_E * sv * sv);
        final double b = (a + alt) * cv;

        ecef[0] = b * cu;
        ecef[1] = b * su;
        ecef[2] = ((1.0 - WGS84_E) * a + alt) * sv;

        return ecef;
    }

    CoordinateConverter(double gst) {
        c = cos(gst);
        s = sin(gst);
    }

    public double[] ecefToEci(double[] ecef, double[] eci) {
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (eci == null) {
            throw new IllegalArgumentException("eci == null");
        }
        if (ecef.length < 2) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length < 2) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length > 6) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length > 6) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (eci.length != ecef.length) {
            throw new IllegalArgumentException("eci.length != ecef.length");
        }

        return ecefToEci(c, s, ecef, eci);
    }

    public double[] eciToEcef(double[] eci, double[] ecef) {
        if (eci == null) {
            throw new IllegalArgumentException("eci == null");
        }
        if (ecef == null) {
            throw new IllegalArgumentException("ecef == null");
        }
        if (eci.length < 2) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length < 2) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (eci.length > 6) {
            throw new IllegalArgumentException("eci.length < 2");
        }
        if (ecef.length > 6) {
            throw new IllegalArgumentException("ecef.length < 2");
        }
        if (ecef.length != eci.length) {
            throw new IllegalArgumentException("ecef.length != eci.length");
        }

        return eciToEcef(c, s, eci, ecef);
    }

    private static double[] ecefToEci(double c, double s, double[] ecef, double[] eci) {
        final double x = ecefToEciX(c, s, ecef[0], ecef[1]);
        final double y = ecefToEciY(c, s, ecef[0], ecef[1]);
        eci[0] = x;
        eci[1] = y;

        if (eci.length == 3) {
            eci[2] = ecef[2];
        } else if (eci.length == 4) {
            final double u = ecefToEciX(c, s, ecef[2], ecef[3]) - WE * y;
            final double v = ecefToEciY(c, s, ecef[2], ecef[3]) + WE * x;
            eci[2] = u;
            eci[3] = v;
        } else if (eci.length == 6) {
            final double u = ecefToEciX(c, s, ecef[3], ecef[4]) - WE * y;
            final double v = ecefToEciY(c, s, ecef[3], ecef[4]) + WE * x;
            eci[2] = ecef[2];
            eci[3] = u;
            eci[4] = v;
            eci[5] = ecef[5];
        }

        return eci;
    }

    private static double[] eciToEcef(double c, double s, double[] eci, double[] ecef) {
        final double x = eciToEcefX(c, s, eci[0], eci[1]);
        final double y = eciToEcefY(c, s, eci[0], eci[1]);

        ecef[0] = x;
        ecef[1] = y;

        if (ecef.length == 3) {
            ecef[2] = eci[2];
        } else if (ecef.length == 4) {
            final double u = eciToEcefX(c, s, eci[2], eci[3]) - WE * y;
            final double v = eciToEcefY(c, s, eci[2], eci[3]) + WE * x;
            ecef[2] = u;
            ecef[3] = v;
        } else if (ecef.length == 6) {
            final double u = eciToEcefX(c, s, eci[3], eci[4]) - WE * y;
            final double v = eciToEcefY(c, s, eci[3], eci[4]) + WE * x;
            ecef[2] = eci[2];
            ecef[3] = u;
            ecef[4] = v;
            ecef[5] = eci[5];
        }

        return ecef;
    }

    private static double ecefToEciX(double c, double s, double ecefX, double ecefY) {
        return c * ecefX - s * ecefY;
    }

    private static double ecefToEciY(double c, double s, double ecefX, double ecefY) {
        return s * ecefX + c * ecefY;
    }

    private static double eciToEcefX(double c, double s, double eciX, double eciY) {
        return c * eciX + s * eciY;
    }

    private static double eciToEcefY(double c, double s, double eciX, double eciY) {
        return c * eciY - s * eciX;
    }
}
