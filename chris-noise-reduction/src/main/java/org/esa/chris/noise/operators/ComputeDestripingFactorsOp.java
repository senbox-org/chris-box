/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.chris.noise.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.chris.dataio.ChrisConstants;
import org.esa.chris.dataio.internal.Sorter;
import org.esa.chris.util.OpUtils;
import org.esa.chris.util.math.internal.LocalRegressionSmoother;
import org.esa.chris.util.math.internal.LowessRegressionWeightCalculator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.Math.acos;
import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

/**
 * Operator for calculating the vertical striping correction factors for noise
 * due to the CCD elements.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 */
@OperatorMetadata(alias = "chris.ComputeDestripingFactors",
                  version = "1.1",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007-2020 by Brockmann Consult",
                  description = "Computes the destriping factors for the given CHRIS/Proba RCIs.")
public class ComputeDestripingFactorsOp extends Operator {

    private static final double G1 = 0.13045510094294;
    private static final double G2 = 0.28135856882126;
    private static final double S1 = -0.12107994955864;
    private static final double S2 = 0.65034734426230;

    @SourceProducts
    Product[] sourceProducts;
    @TargetProduct
    Product targetProduct;

    @Parameter(defaultValue = "27", interval = "[11, 99]")
    int smoothingOrder;

    @Parameter(defaultValue = "true")
    boolean slitCorrection;

    private int spectralBandCount;

    private transient LocalRegressionSmoother smoother;

    private transient Band[][] sourceRciBands;
    private transient Band[][] sourceMskBands;
    private transient Band[] targetBands;
    private transient Panorama panorama;
    private boolean[][] edgeMask;
    private double[] slitNoiseFactors;
    private static final Map<String, Double> DETECTION_THRESHOLD_MAP;

    static {
        DETECTION_THRESHOLD_MAP = new TreeMap<>();
        DETECTION_THRESHOLD_MAP.put("1", 0.08);
        DETECTION_THRESHOLD_MAP.put("2", 0.05);
        DETECTION_THRESHOLD_MAP.put("3", 0.08);
        DETECTION_THRESHOLD_MAP.put("30", 0.08);
        DETECTION_THRESHOLD_MAP.put("3A", 0.08);
        DETECTION_THRESHOLD_MAP.put("4", 0.08);
        DETECTION_THRESHOLD_MAP.put("5", 0.08);
    }


    @Override
    public void initialize() throws OperatorException {
        for (Product sourceProduct : sourceProducts) {
            assertValidity(sourceProduct);
        }

        spectralBandCount = OpUtils.getAnnotationInt(sourceProducts[0], ChrisConstants.ATTR_NAME_NUMBER_OF_BANDS);

        // set up source bands
        sourceRciBands = new Band[spectralBandCount][sourceProducts.length];
        sourceMskBands = new Band[spectralBandCount][sourceProducts.length];

        for (int i = 0; i < spectralBandCount; ++i) {
            final String rciBandName = "radiance_" + (i + 1);
            final String maskBandName = "mask_" + (i + 1);

            for (int j = 0; j < sourceProducts.length; ++j) {
                sourceRciBands[i][j] = sourceProducts[j].getBand(rciBandName);
                sourceMskBands[i][j] = sourceProducts[j].getBand(maskBandName);

                if (sourceRciBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("Could not find band {0}.", rciBandName));
                }
                if (sourceMskBands[i][j] == null) {
                    throw new OperatorException(MessageFormat.format("Could not find band {0}.", maskBandName));
                }
            }
        }

        // set up target product and bands
        targetProduct = new Product("CHRIS_VSC", "CHRIS_VSC", sourceProducts[0].getSceneRasterWidth(), 1);
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 1);

        targetBands = new Band[spectralBandCount];

        for (int i = 0; i < spectralBandCount; ++i) {
            targetBands[i] = targetProduct.addBand("vs_corr_" + (i + 1), ProductData.TYPE_FLOAT64);

            targetBands[i].setSpectralBandIndex(i);
            targetBands[i].setDescription(MessageFormat.format(
                    "Vertical striping correction factors for radiance band {0}", i + 1));
            targetBands[i].setSpectralBandwidth(sourceRciBands[i][0].getSpectralBandwidth());
            targetBands[i].setSpectralWavelength(sourceRciBands[i][0].getSpectralWavelength());
        }

        OpUtils.setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_CHRIS_MODE,
                                    OpUtils.getAnnotationString(sourceProducts[0],
                                                                ChrisConstants.ATTR_NAME_CHRIS_MODE));
        OpUtils.setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE,
                                    OpUtils.getAnnotationString(sourceProducts[0],
                                                                ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE));
        final StringBuilder sb = new StringBuilder("Computed from ");
        for (int i = 0; i < sourceProducts.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(OpUtils.getAnnotationString(sourceProducts[i], ChrisConstants.ATTR_NAME_FLY_BY_ZENITH_ANGLE));
            sb.append("°");
        }
        OpUtils.setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NOISE_REDUCTION, sb.toString());
        final MetadataElement targetBandInfo = new MetadataElement(ChrisConstants.BAND_INFORMATION_NAME);
        ProductUtils.copyMetadata(sourceProducts[0].getMetadataRoot().getElement(ChrisConstants.BAND_INFORMATION_NAME),
                                  targetBandInfo);
        targetProduct.getMetadataRoot().addElement(targetBandInfo);

        panorama = new Panorama(sourceProducts);
        smoother = new LocalRegressionSmoother(new LowessRegressionWeightCalculator(), 2, smoothingOrder, 2);
        if (slitCorrection) {
            slitNoiseFactors = getSlitNoiseFactors(sourceProducts[0]);
        }

        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 1);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            synchronized (this) {
                if (edgeMask == null) {
                    pm.beginTask("Computing correction factors...", 100);
                    edgeMask = createEdgeMask(SubProgressMonitor.create(pm, 90));
                } else {
                    pm.beginTask("Computing correction factors...", 10);
                }
            }
            for (int i = 0; i < targetBands.length; ++i) {
                if (targetBands[i].equals(band)) {
                    computeCorrectionFactors(i, targetTile, SubProgressMonitor.create(pm, 10));
                    return;
                }
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        sourceRciBands = null;
        sourceMskBands = null;
        targetBands = null;
        panorama = null;
        smoother = null;
        edgeMask = null;
        slitNoiseFactors = null;
    }

    /**
     * Computes the vertical striping correction factors for a single target band.
     *
     * @param bandIndex  the band index.
     * @param targetTile the target raster.
     * @param pm         the {@link ProgressMonitor}.
     *
     * @throws OperatorException if an error occurred.
     */
    private void computeCorrectionFactors(int bandIndex, Tile targetTile, ProgressMonitor pm)
            throws OperatorException {
        pm.beginTask("Computing correction factors...", panorama.height + 5);
        try {
            // 1. Accumulate the across-track spatial derivative profile
            final double[] p = new double[panorama.width];
            final int[] count = new int[panorama.width];

            for (int j = 0; j < sourceProducts.length; ++j) {
                final Tile rci = getSceneTile(sourceRciBands[bandIndex][j]);
                final Tile mask = getSceneTile(sourceMskBands[bandIndex][j]);

                for (int y = 0; y < rci.getHeight(); ++y) {
                    checkForCancellation();
                    double r1 = getDouble(rci, 0, y);

                    for (int x = 1; x < rci.getWidth(); ++x) {
                        double r2 = getDouble(rci, x, y);

                        if (!edgeMask[panorama.getY(j, y)][x] && isValid(mask, x, y)) {
                            // prevent NaN values in p ny e.g. division by zero or log(0)
                            r1 = avoidZero(r1);
                            r2 = avoidZero(r2);
                            p[x] += log(r2 / r1);
                            ++count[x];
                        }
                        r1 = r2;
                    }
                    pm.worked(1);
                }
            }
            // 2. Compute the average profile
            for (int x = 1; x < panorama.width; ++x) {
                if (count[x] > 0) {
                    p[x] /= count[x];
                } else {
                    p[x] = p[x - 1];
                }
            }
            pm.worked(1);
            // 3. Compute the integrated profile
            for (int x = 1; x < panorama.width; ++x) {
                p[x] += p[x - 1];
            }
            pm.worked(1);
            // 4. Smooth the integrated profile to get rid of small-scale variations (noise)
            final double[] s = new double[panorama.width];
            smoother.smooth(p, s);
            pm.worked(1);
            // 5. Compute the noise profile
            double meanNoise = 0.0;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= s[x];
                meanNoise += p[x];
            }
            meanNoise /= panorama.width;
            for (int x = 0; x < panorama.width; ++x) {
                p[x] -= meanNoise;
            }
            pm.worked(1);
            // 6. Compute the correction factors
            for (int x = targetTile.getMinX(); x < targetTile.getMinX() + targetTile.getWidth(); ++x) {
                setTargetDouble(targetTile, x, exp(-p[x]));
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private double avoidZero(double r1) {
        if(r1 == 0) {
            return 1;
        }
        return r1;
    }

    private static boolean isValid(Tile mask, int x, int y) {
        return mask.getSampleInt(x, y) == 0;
    }

    private static double[] getSlitNoiseFactors(Product product) throws OperatorException {
        final double[][] table = readSlitVsProfileTable();

        final double[] x = table[0];
        final double[] y = table[1];

        // shift and scale the reference profile according to actual temperature
        final double temperature = OpUtils.getAnnotationDouble(product, ChrisConstants.ATTR_NAME_CHRIS_TEMPERATURE);
        final double scale = G1 * temperature + G2;
        final double shift = S1 * temperature + S2;
        for (int i = 0; i < x.length; ++i) {
            x[i] -= shift;
            y[i] = (y[i] - 1.0) * scale + 1.0;
        }

        int ppc;
        if ("1".equals(OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE))) {
            ppc = 2;
        } else {
            ppc = 1;
        }

        // rebin the profile onto CCD pixels
        double[] f = new double[product.getSceneRasterWidth()];
        for (int pixel = 0, i = 0; pixel < f.length; ++pixel) {
            int count = 0;
            for (; i < x.length; ++i) {
                if (x[i] > (pixel + 1) * ppc + 0.5) {
                    break;
                }
                if (x[i] > 0.5) {
                    f[pixel] += y[i];
                    ++count;
                }
            }
            if (count != 0) {
                f[pixel] /= count;
            } else { // can only happen if the domain of the reference profile is too small
                f[pixel] = 1.0;
            }
        }

        return f;
    }

    /**
     * Creates the spatio-spectral edge mask for a hyperspectral image.
     *
     * @param pm the {@link ProgressMonitor}.
     *
     * @return the edge mask. The value {@code true} indicates changes in the surface
     *         texture or coverage.
     *
     * @throws OperatorException if an error occurred.
     */
    private boolean[][] createEdgeMask(ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Creating edge mask...", spectralBandCount + panorama.width + 2);
        try {

            final double[][] sad = new double[panorama.width][panorama.height];
            final double[][] sca = new double[panorama.width][panorama.height];

            // 1. Compute the squares and across-track scalar products of the spectral vectors
            for (final Band[] bands : sourceRciBands) {
                for (int i = 0; i < bands.length; i++) {
                    final Tile data = getSceneTile(bands[i]);

                    for (int y = 0; y < data.getHeight(); ++y) {
                        checkForCancellation();
                        double r1 = getDouble(data, 0, y);
                        sad[0][panorama.getY(i, y)] += r1 * r1;

                        for (int x = 1; x < data.getWidth(); ++x) {
                            final double r2 = getDouble(data, x, y);

                            sca[x][panorama.getY(i, y)] += r2 * r1;
                            sad[x][panorama.getY(i, y)] += r2 * r2;
                            r1 = r2;
                        }
                    }
                }
                pm.worked(1);
            }
            // 2. Compute the across-track spectral angle differences
            for (int y = 0; y < panorama.height; ++y) {
                checkForCancellation();
                double norm1 = sqrt(sad[0][y]);
                sad[0][y] = 0.0;

                for (int x = 1; x < panorama.width; ++x) {
                    final double norm2 = sqrt(sad[x][y]);

                    sad[x][y] = acos(sca[x][y] / (norm1 * norm2));
                    norm1 = norm2;
                }
            }
            pm.worked(1);

            final int minIndex = (int) (0.60 * panorama.height);
            final int maxIndex = (int) (0.80 * panorama.height);

            double minThreshold = 0.0;
            double maxThreshold = 0.0;

            // 3. Adjust the edge-detection threshold
            for (int x = 1; x < panorama.width; ++x) {
                final double[] values = Arrays.copyOf(sad[x], panorama.height);
                minThreshold = max(minThreshold, Sorter.nthElement(values, minIndex));
                maxThreshold = max(maxThreshold, Sorter.nthElement(values, maxIndex));

                pm.worked(1);
            }
            final double threshold = min(max(getEdgeDetectionThreshold(sourceProducts[0]), minThreshold), maxThreshold);

            // 4. Create the edge mask
            final boolean[][] edgeMask = new boolean[panorama.height][panorama.width];
            for (int y = 0; y < panorama.height; ++y) {
                checkForCancellation();
                for (int x = 1; x < panorama.width; ++x) {
                    if (sad[x][y] > threshold) {
                        edgeMask[y][x] = true;
                    }
                }
            }
            pm.worked(1);

            return edgeMask;
        } finally {
            pm.done();
        }
    }

    private double getDouble(Tile tile, int x, int y) {
        if (slitCorrection) {
            // return the slit-corrected value
            return tile.getSampleDouble(x, y) / slitNoiseFactors[x];
        } else {
            return tile.getSampleDouble(x, y);
        }
    }

    private void setTargetDouble(Tile tile, int x, double v) {
        if (slitCorrection) {
            // combine slit and vertical striping correction into a single correction factor
            tile.setSample(x, 0, v / slitNoiseFactors[x]);
        } else {
            tile.setSample(x, 0, v);
        }
    }

    private Tile getSceneTile(Band band) throws OperatorException {
        return getSourceTile(band, new Rectangle(0, 0, band.getRasterWidth(), band.getRasterHeight()));
    }

    private static double getEdgeDetectionThreshold(Product product) throws OperatorException {
        final String mode = OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);

        if (DETECTION_THRESHOLD_MAP.containsKey(mode)) {
            return DETECTION_THRESHOLD_MAP.get(mode);
        } else {
            throw new OperatorException(MessageFormat.format(
                    "Cannot get edge detection threshold because CHRIS Mode ''{0}'' is not known.", mode));
        }
    }

    private static void assertValidity(Product product) throws OperatorException {
        try {
            OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "Product ''{0}'' is not a CHRIS/Proba product.", product.getName()), e);
        }
    }

    static double[][] readSlitVsProfileTable() throws OperatorException {

        try (ImageInputStream iis = OpUtils.getResourceAsImageInputStream(ComputeDestripingFactorsOp.class,
                                                                          "slit-vs-profile.img")) {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("Cannot read reference slit-VS profile.", e);
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeDestripingFactorsOp.class);
        }
    }


    /**
     * Image panorama.
     */
    private static class Panorama {

        public int width;
        public int height;
        private final Rectangle[] rectangles;

        public Panorama(Product[] products) throws OperatorException {
            width = products[0].getSceneRasterWidth();

            for (Product product : products) {
                if (width != product.getSceneRasterWidth()) {
                    throw new OperatorException("Input products do have inconsistent raster widths");
                }
                height += product.getSceneRasterHeight();
            }

            rectangles = new Rectangle[products.length];

            for (int i = 0, y = 0; i < products.length; ++i) {
                rectangles[i] = new Rectangle(0, y, width, y += products[i].getSceneRasterHeight());
            }
        }

        public final int getY(int i, int y) {
            return rectangles[i].y + y;
        }
    }
}
