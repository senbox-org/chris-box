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
package org.esa.chris.util.math.internal;

/**
 * Performs a linear regression fit.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class Regression {
    private final int m;

    private final double[][] a;
    private final double[][] u;
    private final double[][] v;
    private final double[] s;

    private final int rank;

    public Regression(double[]... x) {
        m = x[0].length;
        int n = x.length;
        a = new double[m][n];

        // build the design matrix
        for (int i = 0; i < m; ++i) {
            final double[] ai = a[i];
            for (int j = 0; j < n; ++j) {
                ai[j] = x[j][i];
            }
        }

        final Jama.SingularValueDecomposition svd = new Jama.Matrix(a, m, n).svd();

        u = svd.getU().getArray();
        v = svd.getV().getArray();
        s = svd.getSingularValues();

        rank = svd.rank();
    }

    public double[] fit(double[] y) {
        return fit(y, new double[m], new double[rank], new double[rank]);
    }

    public double[] fit(double[] y, double[] z) {
        return fit(y, z, new double[rank], new double[rank]);
    }

    public double[] fit(double[] y, double[] z, double[] c) {
        return fit(y, z, c, new double[rank]);
    }

    public double[] fit(double[] y, double[] z, double[] c, double[] w) {
        // compute coefficients
        for (int j = 0; j < rank; ++j) {
            c[j] = 0.0;
            w[j] = 0.0;
            for (int i = 0; i < m; ++i) {
                w[j] += u[i][j] * y[i];
            }
            w[j] /= s[j];
        }
        for (int j = 0; j < rank; ++j) {
            final double[] vj = v[j];

            for (int i = 0; i < rank; ++i) {
                c[j] += vj[i] * w[i];
            }
        }
        // compute fit values
        for (int j = 0; j < m; ++j) {
            z[j] = 0.0;
        }
        for (int i = 0; i < rank; ++i) {
            final double ci = c[i];

            for (int j = 0; j < m; ++j) {
                z[j] += ci * a[j][i];
            }
        }

        return z;
    }
}
