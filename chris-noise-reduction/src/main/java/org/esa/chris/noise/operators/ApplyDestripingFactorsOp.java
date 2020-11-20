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
import org.esa.chris.util.OpUtils;
import org.esa.chris.dataio.ChrisConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;
import java.text.MessageFormat;

/**
 * Operator for applying the vertical striping (VS) correction factors calculated by
 * the {@link ComputeDestripingFactorsOp}.
 *
 * @author Ralf Quast
 * @author Marco Zühlke
 */
@OperatorMetadata(alias = "chris.ApplyDestripingFactors",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2007-2020 by Brockmann Consult",
                  description = "Applies a precomputed set of destriping factors to a CHRIS/Proba RCI.")
public class ApplyDestripingFactorsOp extends Operator {

    @SourceProduct(alias = "input")
    Product sourceProduct;
    @SourceProduct(alias = "factors")
    Product factorProduct;
    @TargetProduct
    Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        assertValidity(sourceProduct);

        targetProduct = new Product(sourceProduct.getName() + "_NR", sourceProduct.getProductType() + "_NR",
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);

        for (final Band sourceBand : sourceProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);

            final FlagCoding flagCoding = sourceBand.getFlagCoding();
            if (flagCoding != null) {
                targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
            }
        }
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        OpUtils.setAnnotationString(targetProduct, ChrisConstants.ATTR_NAME_NOISE_REDUCTION,
                                    OpUtils.getAnnotationString(factorProduct,
                                                                ChrisConstants.ATTR_NAME_NOISE_REDUCTION));
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 16);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final String name = band.getName();
        if (name.startsWith("radiance")) {
            computeRciBand(name, targetTile, pm);
        } else {
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(name), targetTile.getRectangle());
            targetTile.setRawSamples(sourceTile.getRawSamples());
        }
    }

    private void computeRciBand(String name, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("removing vertical striping artifacts", targetTile.getHeight());
        try {
            final Band sourceBand = sourceProduct.getBand(name);
            final Band factorBand = factorProduct.getBand(name.replace("radiance", "vs_corr"));

            final Rectangle targetRectangle = targetTile.getRectangle();
            final Rectangle factorRectangle = new Rectangle(targetRectangle.x, 0, targetRectangle.width, 1);

            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle);
            final Tile factorTile = getSourceTile(factorBand, factorRectangle);

            final int[] sourceSamples = sourceTile.getDataBufferInt();
            final int[] targetSamples = targetTile.getDataBufferInt();
            final double[] factorSamples = factorTile.getDataBufferDouble();

            int sourceOffset = sourceTile.getScanlineOffset();
            int factorOffset = factorTile.getScanlineOffset();
            int targetOffset = targetTile.getScanlineOffset();

            for (int y = 0; y < targetTile.getHeight(); ++y) {
                checkForCancellation();

                int sourceIndex = sourceOffset;
                int factorIndex = factorOffset;
                int targetIndex = targetOffset;
                for (int x = 0; x < targetTile.getWidth(); ++x) {
                    targetSamples[targetIndex] = (int) (sourceSamples[sourceIndex] * factorSamples[factorIndex] + 0.5);
                    ++sourceIndex;
                    ++factorIndex;
                    ++targetIndex;
                }
                sourceOffset += sourceTile.getScanlineStride();
                targetOffset += targetTile.getScanlineStride();

                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private static void assertValidity(Product product) throws OperatorException {
        try {
            OpUtils.getAnnotationString(product, ChrisConstants.ATTR_NAME_CHRIS_MODE);
        } catch (OperatorException e) {
            throw new OperatorException(MessageFormat.format(
                    "product ''{0}'' is not a CHRIS product", product.getName()), e);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ApplyDestripingFactorsOp.class);
        }
    }
}
