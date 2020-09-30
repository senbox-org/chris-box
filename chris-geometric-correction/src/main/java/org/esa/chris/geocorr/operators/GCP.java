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

import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.ProductNodeGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Ground control point.
 *
 * @author Ralf Quast
 * @since CHRIS-Box 1.5
 */
class GCP {

    private final double x;
    private final double y;
    private final double lon;
    private final double lat;
    private final double alt;

    GCP(double x, double y, double lon, double lat, double alt) {
        this.x = x;
        this.y = y;
        this.lon = lon;
        this.lat = lat;
        this.alt = alt;
    }

    static GCP create(Placemark placemark, double defaultAltitude) {
        final double x = placemark.getPixelPos().getX();
        final double y = placemark.getPixelPos().getY();
        final double lon = placemark.getGeoPos().getLon();
        final double lat = placemark.getGeoPos().getLat();
        final double alt = parseAltitude(placemark.getDescription(), defaultAltitude);

        return new GCP(x, y, lon, lat, alt);
    }

    static GCP[] createArray(ProductNodeGroup<Placemark> placemarkGroup, double defaultAltitude) {
        final int placemarkCount = placemarkGroup.getNodeCount();
        final List<GCP> gcpList = new ArrayList<>(placemarkCount);
        for (final Placemark placemark : placemarkGroup.toArray(new Placemark[placemarkCount])) {
            if (isValid(placemark)) {
                gcpList.add(create(placemark, defaultAltitude));
            }
        }
        return gcpList.toArray(new GCP[0]);
    }

    int getCol() {
        return (int) x;
    }

    int getRow() {
        return (int) y;
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    double getLon() {
        return lon;
    }

    double getLat() {
        return lat;
    }

    double getAlt() {
        return alt;
    }

    static double parseAltitude(String description, double defaultAltitude) {
        final int i = description.indexOf("(alt");
        if (i != -1) {
            final int k = description.indexOf("=", i);
            if (k != -1) {
                final int l = description.indexOf(")", k);
                try {
                    double alt = Double.parseDouble(description.substring(k + 1, l));
                    if (alt > 10) {
                        // altitude is given in meter
                        alt /= 1000.0;
                    }
                    return alt;
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return defaultAltitude;
    }

    private static boolean isValid(Placemark placemark) {
        return placemark.getPixelPos() != null
               && placemark.getPixelPos().isValid()
               && placemark.getGeoPos() != null
               && placemark.getGeoPos().isValid();
    }
}
