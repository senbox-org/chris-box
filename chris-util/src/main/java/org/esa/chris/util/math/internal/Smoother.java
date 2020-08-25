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

public interface Smoother {

    /**
     * Smoothes a given set of values.
     *
     * @param values         the values.
     * @param smoothedValues the smoothed values.
     *
     * @throws IllegalArgumentException if the lengths of {@code values} and {@code smoothedValues} are
     *                                  different.
     */
    void smooth(double[] values, double[] smoothedValues);
}
