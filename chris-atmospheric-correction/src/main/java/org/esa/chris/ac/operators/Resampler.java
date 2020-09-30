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

/**
 * Resampler.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class Resampler {

    private final int targetWavelengthCount;
    private final double[][] weights;

    /**
     * Creates a new resampler for resampling spectral quantities given for
     * certain source wavelengths to specific target bands.
     *
     * @param sourceWavelengths the source wavelengths.
     * @param targetWavelengths the target wavelengths.
     * @param targetBandwidths  the target bandwidths.
     */
    public Resampler(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths) {
        this(sourceWavelengths, targetWavelengths, targetBandwidths, 0.0);
    }

    /**
     * Creates a new resampler for resampling spectral quantities given for
     * certain source wavelengths to specific target bands.
     *
     * @param sourceWavelengths the source wavelengths.
     * @param targetWavelengths the target wavelengths.
     * @param targetBandwidths  the target bandwidths.
     * @param targetShift       the target wavelength shift.
     */
    public Resampler(double[] sourceWavelengths, double[] targetWavelengths, double[] targetBandwidths,
                     double targetShift) {
        targetWavelengthCount = targetWavelengths.length;

        weights = calculateResamplingWeights(sourceWavelengths, targetWavelengths, targetBandwidths,
                                             targetShift, new double[targetWavelengthCount][sourceWavelengths.length]);
    }

    /**
     * Resamples the given spectral values.
     *
     * @param sourceValues the spectral values.
     *
     * @return the resampled values.
     */
    public double[] resample(double[] sourceValues) {
        return multiply(sourceValues, weights, new double[targetWavelengthCount]);
    }

    /**
     * Multiplies a vector by the rows of a matrix. The resulting vector has the same
     * number of components as the matrix has rows.
     *
     * @param a the vector.
     * @param b the matrix.
     * @param c the resulting matrix (overwritten on output).
     *
     * @return the resulting matrix.
     */
    private static double[] multiply(double[] a, double[][] b, double[] c) {
        for (int i = 0; i < b.length; ++i) {
            double sum = 0.0;
            for (int k = 0; k < a.length; ++k) {
                sum += a[k] * b[i][k];
            }
            c[i] = sum;
        }

        return c;
    }

    /**
     * Calculates the weights for resampling spectral quantities computed at
     * wavelengths {@code sourceWavelenghts}  to spectral bands with central
     * wavelenghts {@code targetWavelengths} and bandwidths {@code targetBandwidths}.
     *
     * @param sourceWavelengths the source wavelenghts.
     * @param targetWavelengths the targetWavelengths.
     * @param targetBandwidths  the target bandwidths.
     * @param targetShift       the target wavelength shift.
     * @param weights           the resampling weights (overwritten on output).
     *
     * @return the resampling weights.
     */
    private static double[][] calculateResamplingWeights(double[] sourceWavelengths,
                                                         double[] targetWavelengths,
                                                         double[] targetBandwidths, double targetShift,
                                                         double[][] weights) {
        final int sourceWavelengthCount = sourceWavelengths.length;
        final int targetWavelengthCount = targetWavelengths.length;

        final double max = 6.0;
        final double min = 2.0;

        final double[] e = new double[targetWavelengthCount];
        final double[] c = new double[targetWavelengthCount];

        for (int i = 0; i < targetWavelengthCount; ++i) {
            e[i] = max + ((min - max) * i) / (targetWavelengthCount - 1);
            c[i] = Math.pow(1.0 / (Math.pow(2.0, e[i]) * Math.log(2.0)), 1.0 / e[i]);
        }

        for (int i = 0; i < targetWavelengthCount; ++i) {
            for (int j = 0; j < sourceWavelengthCount; ++j) {
                final double delta = Math.abs(targetWavelengths[i] + targetShift - sourceWavelengths[j]);
                if (delta <= 2.0 * targetBandwidths[i]) {
                    weights[i][j] = 1.0 / Math.exp(Math.pow(delta / (targetBandwidths[i] * c[i]), e[i]));
                }
            }
            // normalize weights
            double sum = 0.0;
            for (int j = 0; j < sourceWavelengthCount; ++j) {
                sum += weights[i][j];
            }
            if (sum > 0.0) {
                for (int j = 0; j < sourceWavelengthCount; ++j) {
                    weights[i][j] /= sum;
                }
            }
        }

        return weights;
    }
}
