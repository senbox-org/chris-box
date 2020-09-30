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

package org.esa.chris.refl.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.chris.util.OpUtils;
import org.esa.chris.dataio.ChrisConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Operator for computing TOA reflectances from CHRIS response corrected
 * images.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "chris.ComputeToaReflectances",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007 by Brockmann Consult",
                  description = "Computes TOA reflectances from a CHRIS/Proba RCI.")
public class ComputeToaReflectancesOp extends Operator {

    private static final String TOA_REFL = "toa_refl";
    private static final double TOA_REFL_SCALING_FACTOR = 1.0E-4;

    @SourceProduct(alias = "source", type = "CHRIS_M.*")
    private Product sourceProduct;

    @SuppressWarnings({"FieldCanBeLocal"})
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false",
               label = "Copy radiance bands",
               description = "If 'true' all radiance bands from the source product are copied to the target product.")
    private boolean copyRadianceBands;

    private transient Map<Band, Band> sourceBandMap;
    private transient Map<Band, Double> conversionFactorMap;

    @Override
    public void initialize() throws OperatorException {
        sourceBandMap = new HashMap<Band, Band>(sourceProduct.getNumBands());
        conversionFactorMap = new HashMap<Band, Double>(sourceProduct.getNumBands());

        final double solarZenithAngle = OpUtils.getAnnotationDouble(sourceProduct,
                                                                    ChrisConstants.ATTR_NAME_SOLAR_ZENITH_ANGLE);
        final double[][] table = OpUtils.readThuillierTable();
        final int day = OpUtils.getAcquisitionDay(sourceProduct);
        computeSolarIrradianceTable(table, day);

        final String type = sourceProduct.getProductType() + "_TOA_REFL";
        targetProduct = new Product("CHRIS_TOA_REFL", type,
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        if (copyRadianceBands) {
            for (final Band sourceBand : sourceProduct.getBands()) {
                if (sourceBand.getName().startsWith("radiance")) {
                    final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
                    final double solarIrradiance = getAverageValue(table,
                                                                   sourceBand.getSpectralWavelength(),
                                                                   sourceBand.getSpectralBandwidth());
                    targetBand.setSolarFlux((float) solarIrradiance);
                }
            }
        }
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand.getName().startsWith("radiance")) {
                final Band targetBand = new Band(sourceBand.getName().replaceFirst("radiance", TOA_REFL),
                                                 ProductData.TYPE_INT16,
                                                 sourceBand.getRasterWidth(),
                                                 sourceBand.getRasterHeight());

                targetBand.setDescription(MessageFormat.format(
                        "TOA Reflectance for spectral band {0}", sourceBand.getSpectralBandIndex() + 1));
                targetBand.setUnit("dl");
                targetBand.setScalingFactor(TOA_REFL_SCALING_FACTOR);
                targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());
                targetBand.setSpectralBandIndex(sourceBand.getSpectralBandIndex());
                targetBand.setSpectralWavelength(sourceBand.getSpectralWavelength());
                targetBand.setSpectralBandwidth(sourceBand.getSpectralBandwidth());
                final double solarIrradiance = getAverageValue(table,
                                                               sourceBand.getSpectralWavelength(),
                                                               sourceBand.getSpectralBandwidth());
                targetBand.setSolarFlux((float) solarIrradiance);
                targetProduct.addBand(targetBand);

                final double conversionFactor = PI / (cos(toRadians(solarZenithAngle)) * 1000.0 * solarIrradiance);
                conversionFactorMap.put(targetBand, conversionFactor);
                sourceBandMap.put(targetBand, sourceBand);
            } else if (sourceBand.getName().startsWith("mask")) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
                final double solarIrradiance = getAverageValue(table,
                                                               sourceBand.getSpectralWavelength(),
                                                               sourceBand.getSpectralBandwidth());
                targetBand.setSolarFlux((float) solarIrradiance);
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
                }
            } else {
                ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, true);
            }
        }
        if (copyRadianceBands) {
            targetProduct.setAutoGrouping("radiance:mask:"+TOA_REFL);
        } else {
            targetProduct.setAutoGrouping("mask:"+TOA_REFL);
        }

        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());

        // DO NOT CHANGE TILE SIZE - IT IS NEEDED FOR PROGRESS MONITORING IN THE CLOUD SCREENING!
        targetProduct.setPreferredTileSize(32, 32);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        if (sourceBandMap.containsKey(targetBand)) {
            computeReflectances(targetBand, targetTile, pm);
        }
    }

    @Override
    public void dispose() {
        sourceBandMap.clear();
        sourceBandMap = null;

        conversionFactorMap.clear();
        conversionFactorMap = null;
    }

    private void computeReflectances(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask("computing reflectances...", targetTile.getHeight());

            final Band sourceBand = sourceBandMap.get(targetBand);
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle);

            final double conversionFactor = conversionFactorMap.get(targetBand);

            final int[] sourceSamples = sourceTile.getDataBufferInt();
            final short[] targetSamples = targetTile.getDataBufferShort();

            int sourceOffset = sourceTile.getScanlineOffset();
            int sourceStride = sourceTile.getScanlineStride();
            int targetOffset = targetTile.getScanlineOffset();
            int targetStride = targetTile.getScanlineStride();

            for (int y = 0; y < targetTile.getHeight(); ++y) {
                int sourceIndex = sourceOffset;
                int targetIndex = targetOffset;

                for (int x = 0; x < targetTile.getWidth(); ++x) {
                    checkForCancellation();

                    targetSamples[targetIndex] = (short) (sourceSamples[sourceIndex] * conversionFactor /
                                                          TOA_REFL_SCALING_FACTOR + 0.5);

                    ++sourceIndex;
                    ++targetIndex;
                }
                sourceOffset += sourceStride;
                targetOffset += targetStride;

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    // todo - move or make an averager class

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


    /**
     * Computes the solar irradiance for a given acquisition day.
     *
     * @param table the nominal solar irradiance table. On output contains the
     *              solar irradiance for the given acquisition day.
     * @param day   the acquisition day number.
     */
    private static void computeSolarIrradianceTable(double[][] table, int day) {
        final double[] irradiances = table[1];

        final double factor = OpUtils.getSolarIrradianceCorrectionFactor(day);

        for (int i = 0; i < irradiances.length; ++i) {
            irradiances[i] *= factor;
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ComputeToaReflectancesOp.class);
        }
    }
}
