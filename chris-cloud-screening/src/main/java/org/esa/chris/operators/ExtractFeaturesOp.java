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

package org.esa.chris.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.chris.dataio.ChrisConstants;
import org.esa.chris.util.BandFilter;
import org.esa.chris.util.OpUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.toRadians;

/**
 * Operator for extracting features from TOA reflectances needed for
 * cloud screening.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ExtractFeatures",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Extracts features from TOA reflectances needed for cloud screening.")
public class ExtractFeaturesOp extends Operator {

    private static final double INVERSE_SCALING_FACTOR = 10000.0;

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    private transient Band br;
    private transient Band wh;
    private transient Band visBr;
    private transient Band visWh;
    private transient Band nirBr;
    private transient Band nirWh;
    private transient Band o2;
    private transient Band wv;

    private transient Band[] surfaceBands;
    private transient Band[] visBands;
    private transient Band[] nirBands;

    private transient boolean canComputeAtmosphericFeatures;
    private transient double trO2;
    private transient double trWv;
    private transient BandInterpolator interpolatorO2;
    private transient BandInterpolator interpolatorWv;
    private transient double mu;

    @Override
    public void initialize() throws OperatorException {
        final Band[] reflectanceBands = OpUtils.findBands(sourceProduct, "toa_refl");

        if (reflectanceBands.length == 0) {
            throw new OperatorException("Cannot not find source bands.");
        }
        categorizeBands(reflectanceBands);

        canComputeAtmosphericFeatures = sourceProduct.getProductType().matches("CHRIS_M[15].*");

        if (canComputeAtmosphericFeatures) {
            interpolatorO2 = new BandInterpolator(reflectanceBands,
                                                  new double[]{760.625, 755.0, 770.0, 738.0, 755.0, 770.0, 788.0});
            interpolatorWv = new BandInterpolator(reflectanceBands,
                                                  new double[]{944.376, 895.0, 960.0, 865.0, 890.0, 985.0, 1100.0});
            final double[][] transmittanceTable = readTransmittanceTable();
            trO2 = getAverageValue(transmittanceTable, interpolatorO2.getInnerWavelength(),
                                   interpolatorO2.getInnerBandwidth());
            trWv = getAverageValue(transmittanceTable, interpolatorWv.getInnerWavelength(),
                                   interpolatorWv.getInnerBandwidth());

            final double sza = OpUtils.getAnnotationDouble(sourceProduct, ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
            final double vza = OpUtils.getAnnotation(sourceProduct, ChrisConstants.ATTR_NAME_OBSERVATION_ZENITH_ANGLE,
                                                     0.0);
            mu = 1.0 / (1.0 / cos(toRadians(sza)) + 1.0 / cos(toRadians(vza)));
        }

        final String type = sourceProduct.getProductType() + "_FEAT";
        targetProduct = new Product("CHRIS_FEATURES", type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        br = targetProduct.addBand("brightness", ProductData.TYPE_INT16);
        br.setDescription("Brightness for visual and NIR bands");
        br.setUnit("dl");
        br.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visBr = targetProduct.addBand("brightness_vis", ProductData.TYPE_INT16);
        visBr.setDescription("Brightness for visual bands");
        visBr.setUnit("dl");
        visBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirBr = targetProduct.addBand("brightness_nir", ProductData.TYPE_INT16);
        nirBr.setDescription("Brightness for NIR bands");
        nirBr.setUnit("dl");
        nirBr.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        wh = targetProduct.addBand("whiteness", ProductData.TYPE_INT16);
        wh.setDescription("Whiteness for visual and NIR bands");
        wh.setUnit("dl");
        wh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        visWh = targetProduct.addBand("whiteness_vis", ProductData.TYPE_INT16);
        visWh.setDescription("Whiteness for visual bands");
        visWh.setUnit("dl");
        visWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        nirWh = targetProduct.addBand("whiteness_nir", ProductData.TYPE_INT16);
        nirWh.setDescription("Whiteness for NIR bands");
        nirWh.setUnit("dl");
        nirWh.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

        if (canComputeAtmosphericFeatures) {
            o2 = targetProduct.addBand("o2", ProductData.TYPE_INT16);
            o2.setDescription("Atmospheric oxygen absorption");
            o2.setUnit("dl");
            o2.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);

            wv = targetProduct.addBand("wv", ProductData.TYPE_INT16);
            wv.setDescription("Atmospheric water vapour absorption");
            wv.setUnit("dl");
            wv.setScalingFactor(1.0 / INVERSE_SCALING_FACTOR);
        }

        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        // DO NOT CHANGE TILE SIZE - IT IS NEEDED FOR PROGRESS MONITORING!
        targetProduct.setPreferredTileSize(32, 32);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        if (canComputeAtmosphericFeatures) {
            pm.beginTask("computing bands...", 8);
        } else {
            pm.beginTask("computing bands...", 6);
        }
        try {
            computeSurfaceFeatures(br, wh, targetTileMap, targetRectangle, surfaceBands,
                                   SubProgressMonitor.create(pm, 2));
            computeSurfaceFeatures(visBr, visWh, targetTileMap, targetRectangle, visBands,
                                   SubProgressMonitor.create(pm, 2));
            computeSurfaceFeatures(nirBr, nirWh, targetTileMap, targetRectangle, nirBands,
                                   SubProgressMonitor.create(pm, 2));
            if (canComputeAtmosphericFeatures) {
                computeAtmosphericFeature(o2, targetTileMap, targetRectangle, interpolatorO2, trO2,
                                          SubProgressMonitor.create(pm, 1));
                computeAtmosphericFeature(wv, targetTileMap, targetRectangle, interpolatorWv, trWv,
                                          SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void dispose() {
        br = null;
        wh = null;
        visBr = null;
        visWh = null;
        nirBr = null;
        nirWh = null;
        o2 = null;
        wv = null;

        surfaceBands = null;
        visBands = null;
        nirBands = null;

        interpolatorO2 = null;
        interpolatorWv = null;
    }

    private void categorizeBands(Band[] bands) {
        final List<Band> surfaceBandList = new ArrayList<>(bands.length);
        final List<Band> visBandList = new ArrayList<>(bands.length);
        final List<Band> nirBandList = new ArrayList<>(bands.length);

        final BandFilter visBandFilter = new InclusiveBandFilter(400.0, 700.0);
        final BandFilter absBandFilter = new InclusiveMultiBandFilter(new double[][]{
                {400.0, 440.0},
                {590.0, 600.0},
                {630.0, 636.0},
                {648.0, 658.0},
                {686.0, 709.0},
                {792.0, 799.0},
                {756.0, 775.0},
                {808.0, 840.0},
                {885.0, 985.0},
                {985.0, 1010.0}
        });

        for (final Band band : bands) {
            if (absBandFilter.accept(band)) {
                continue;
            }
            surfaceBandList.add(band);
            if (visBandFilter.accept(band)) {
                visBandList.add(band);
            } else {
                nirBandList.add(band);
            }
        }

        if (surfaceBandList.isEmpty()) {
            throw new OperatorException("no absorption-free bands found");
        }
        if (visBandList.isEmpty()) {
            throw new OperatorException("no absorption-free visual bands found");
        }
        if (nirBandList.isEmpty()) {
            throw new OperatorException("no absorption-free NIR bands found");
        }

        surfaceBands = surfaceBandList.toArray(new Band[0]);
        visBands = visBandList.toArray(new Band[0]);
        nirBands = nirBandList.toArray(new Band[0]);
    }

    void computeSurfaceFeatures(Band bBand, Band wBand, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                Band[] sourceBands, ProgressMonitor pm) {
        pm.beginTask("computing surface features...", targetRectangle.height);
        try {
            final double[] wavelengths = getSpectralWavelengths(sourceBands);
            final Tile[] sourceTiles = getSourceTiles(sourceBands, targetRectangle);

            final Tile bTile = targetTileMap.get(bBand);
            final Tile wTile = targetTileMap.get(wBand);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                    checkForCancellation();

                    final double[] reflectances = getSamples(x, y, sourceTiles);
                    final double b = brightness(wavelengths, reflectances);
                    final double w = whiteness(wavelengths, reflectances, b);

                    bTile.setSample(x, y, b);
                    wTile.setSample(x, y, w);
                }

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private Tile[] getSourceTiles(Band[] bands, Rectangle targetRectangle) {
        final Tile[] sourceTiles = new Tile[bands.length];

        for (int i = 0; i < bands.length; ++i) {
            sourceTiles[i] = getSourceTile(bands[i], targetRectangle);
        }
        return sourceTiles;
    }

    private static double[] getSpectralWavelengths(Band[] bands) {
        final double[] wavelengths = new double[bands.length];

        for (int i = 0; i < bands.length; ++i) {
            wavelengths[i] = bands[i].getSpectralWavelength();
        }
        return wavelengths;
    }

    private void computeAtmosphericFeature(Band targetBand, Map<Band, Tile> targetTileMap, Rectangle targetRectangle,
                                           BandInterpolator bandInterpolator, double transmittance,
                                           ProgressMonitor pm) {
        pm.beginTask("computing optical path...", targetRectangle.height);
        try {
            final Band sourceBand = bandInterpolator.getInnerBand();
            final Band[] infBands = bandInterpolator.getInfBands();
            final Band[] supBands = bandInterpolator.getSupBands();

            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle);
            final Tile targetTile = targetTileMap.get(targetBand);

            final Tile[] infTiles = getSourceTiles(infBands, targetRectangle);
            final Tile[] supTiles = getSourceTiles(supBands, targetRectangle);

            final double c = mu / log(transmittance);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                    checkForCancellation();

                    final double a = getMean(x, y, infTiles);
                    final double b = getMean(x, y, supTiles);
                    final double f = c * log(sourceTile.getSampleDouble(x, y) / bandInterpolator.getValue(a, b));

                    targetTile.setSample(x, y, f);
                }

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static double[] getSamples(int x, int y, Tile[] tiles) {
        final double[] samples = new double[tiles.length];

        for (int i = 0; i < samples.length; i++) {
            samples[i] = tiles[i].getSampleDouble(x, y);
        }

        return samples;
    }

    private static double getMean(int x, int y, Tile[] tiles) {
        double sum = 0.0;

        for (final Tile tile : tiles) {
            sum += tile.getSampleDouble(x, y);
        }

        return sum / tiles.length;
    }

    private static double getAverageValue(double[][] table, double wavelength, double width) {
        final double[] x = table[0];
        final double[] y = table[1];

        double ws = 0.0;
        double ys = 0.0;

        for (int i = 0; i < table[0].length; ++i) {
            if (x[i] > wavelength + width) {
                break;
            }
            if (x[i] > wavelength - width) {
                final double w = 1.0 / pow(1.0 + abs(2.0 * (x[i] - wavelength) / width), 4.0);

                ys += y[i] * w;
                ws += w;
            }
        }

        return ys / ws;
    }

    private static double brightness(double[] wavelengths, double[] reflectances) {
        double sum = 0.0;

        for (int i = 1; i < reflectances.length; ++i) {
            sum += 0.5 * (reflectances[i] + reflectances[i - 1]) * (wavelengths[i] - wavelengths[i - 1]);
        }

        return sum / (wavelengths[wavelengths.length - 1] - wavelengths[0]);
    }

    private static double whiteness(double[] wavelengths, double[] reflectances, double brightness) {
        double sum = 0.0;

        for (int i = 1; i < reflectances.length; i++) {
            final double y1 = reflectances[i - 1] - brightness;
            final double y2 = reflectances[i] - brightness;

            // trapezoidal integration
            final double x1 = wavelengths[i - 1];
            final double x2 = wavelengths[i];
            
            if (y1 >= 0.0 && y2 >= 0.0 || y1 <= 0.0 && y2 <= 0.0) {
                // abscissa is not intersected
                sum += 0.5 * (abs(y1) + abs(y2)) * (x2 - x1);
            } else {
                // abscissa is intersected at
                final double x0 = x1 - y1 * (x2 - x1) / (y2 - y1);
                // sum of two triangles
                sum += 0.5 * (abs(y1) * (x0 - x1) + abs(y2) * (x2 - x0));
            }
        }

        return sum / (wavelengths[wavelengths.length - 1] - wavelengths[0]);
    }

    static double[][] readTransmittanceTable() throws OperatorException {

        try (ImageInputStream iis = OpUtils.getResourceAsImageInputStream(ExtractFeaturesOp.class,
                                                                          "nir-transmittance.img")) {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (Exception e) {
            throw new OperatorException("could not read NIR transmittance table", e);
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractFeaturesOp.class);
        }
    }


    private static class BandInterpolator {

        private final Band innerBand;
        private final Band[] infBands;
        private final Band[] supBands;

        private final double interpolationWeight;

        public BandInterpolator(Band[] bands, double[] wavelengths) {
            innerBand = findProximateBand(bands, wavelengths[0], new StrictlyInclusiveBandFilter(wavelengths[1],
                                                                                                 wavelengths[2]));

            infBands = findBands(bands, new StrictlyInclusiveBandFilter(wavelengths[3], wavelengths[4]));
            supBands = findBands(bands, new StrictlyInclusiveBandFilter(wavelengths[5], wavelengths[6]));

            if (innerBand == null) {
                throw new OperatorException(MessageFormat.format(
                        "no absorption band found for wavelength {0} nm", wavelengths[0]));
            }
            if (infBands.length == 0 && supBands.length == 0) {
                throw new OperatorException(MessageFormat.format(
                        "no interpolation bands found for wavelength {0} nm", wavelengths[0]));
            }
            final double a = meanWavelength(infBands);
            final double b = meanWavelength(supBands);

            interpolationWeight = (innerBand.getSpectralWavelength() - a) / (b - a);
        }

        public final Band getInnerBand() {
            return innerBand;
        }

        public double getInnerWavelength() {
            return innerBand.getSpectralWavelength();
        }

        public double getInnerBandwidth() {
            return innerBand.getSpectralBandwidth();
        }

        public final Band[] getInfBands() {
            return infBands;
        }

        public final Band[] getSupBands() {
            return supBands;
        }

        public double getValue(double a, double b) {
            if (infBands.length == 0) {
                return b;
            }
            if (supBands.length == 0) {
                return a;
            }

            return (1.0 - interpolationWeight) * a + interpolationWeight * b;
        }

        private static Band[] findBands(Band[] bands, BandFilter bandFilter) {
            final List<Band> bandList = new ArrayList<>();

            for (final Band band : bands) {
                if (bandFilter.accept(band)) {
                    bandList.add(band);
                }
            }

            return bandList.toArray(new Band[0]);
        }

        private static Band findProximateBand(Band[] bands, double wavelength, BandFilter bandFilter) {
            Band proximateBand = null;

            for (final Band band : bands) {
                if (bandFilter.accept(band)) {
                    if (proximateBand == null || dist(proximateBand, wavelength) > dist(band, wavelength)) {
                        proximateBand = band;
                    }
                }
            }

            return proximateBand;
        }

        private static double dist(Band band, double wavelength) {
            return abs(band.getSpectralWavelength() - wavelength);
        }

        private static double meanWavelength(Band[] bands) {
            double sum = 0.0;

            for (final Band band : bands) {
                sum += band.getSpectralWavelength();
            }

            return sum / bands.length;
        }
    }
}
