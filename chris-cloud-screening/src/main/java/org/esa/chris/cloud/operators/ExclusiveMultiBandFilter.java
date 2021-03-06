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
package org.esa.chris.cloud.operators;

import org.esa.chris.util.BandFilter;
import org.esa.snap.core.datamodel.Band;

/**
 * Exclusive multi band filter.
 *
 * @author Ralf Quast
 */
class ExclusiveMultiBandFilter implements BandFilter {
    private final InclusiveMultiBandFilter inclusiveMultiBandFilter;

    ExclusiveMultiBandFilter(double[]... wavelengthIntervals) {
        inclusiveMultiBandFilter = new InclusiveMultiBandFilter(wavelengthIntervals);
    }

    @Override
    public boolean accept(Band band) {
        return !inclusiveMultiBandFilter.accept(band);
    }
}
