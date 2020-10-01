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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.BandOpImage;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.OpImage;
import javax.media.jai.PixelAccessor;
import javax.media.jai.PlanarImage;
import javax.media.jai.PointOpImage;
import javax.media.jai.UnpackedImageData;
import java.awt.Rectangle;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Vector;

/**
 * Hyper-spectral mask image.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
class HyperMaskOpImage extends PointOpImage {

    /**
     * Creates a hyper-spectral mask image by combining the least significant bytes
     * of the CHRIS mask bands supplied.
     *
     * @param maskBands the mask bands.
     *
     * @return the hyper-spectral mask image.
     */
    public static OpImage createImage(Band[] maskBands) {
        final Vector<RenderedImage> sourceImageVector = new Vector<>();

        for (final Band maskBand : maskBands) {
            RenderedImage image = maskBand.getSourceImage();
            if (image == null) {
                image = new BandOpImage(maskBand);
                maskBand.setSourceImage(image);
            }
            sourceImageVector.add(image);
        }

        int w = maskBands[0].getRasterWidth();
        int h = maskBands[1].getRasterHeight();

        final SampleModel sampleModel = new ComponentSampleModelJAI(DataBuffer.TYPE_BYTE, w, h, 1, w, new int[]{0});
        final ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        final ImageLayout imageLayout = new ImageLayout(0, 0, w, h, 0, 0, w, h, sampleModel, colorModel);

        return new HyperMaskOpImage(imageLayout, sourceImageVector);
    }

    private HyperMaskOpImage(ImageLayout imageLayout, Vector<RenderedImage> sourceImageVector) {
        super(sourceImageVector, imageLayout, null, true);
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster target, Rectangle rectangle) {
        final PixelAccessor targetAccessor;
        final UnpackedImageData targetData;
        final byte[] targetPixels;

        targetAccessor = new PixelAccessor(getSampleModel(), getColorModel());
        targetData = targetAccessor.getPixels(target, rectangle, DataBuffer.TYPE_BYTE, true);
        targetPixels = targetData.getByteData(0);

        for (int i = 0; i < sources.length; ++i) {
            final PixelAccessor sourceAccessor;
            final UnpackedImageData sourceData;
            final short[] sourcePixels;

            sourceAccessor = new PixelAccessor(getSourceImage(i));
            sourceData = sourceAccessor.getPixels(sources[i], rectangle, DataBuffer.TYPE_SHORT, false);
            sourcePixels = sourceData.getShortData(0);

            int sourceLineOffset = sourceData.bandOffsets[0];
            int targetLineOffset = targetData.bandOffsets[0];

            for (int y = 0; y < rectangle.height; ++y) {
                int sourcePixelOffset = sourceLineOffset;
                int targetPixelOffset = targetLineOffset;

                for (int x = 0; x < rectangle.width; ++x) {
                    targetPixels[targetPixelOffset] |= (sourcePixels[sourcePixelOffset] & 255);

                    sourcePixelOffset += sourceData.pixelStride;
                    targetPixelOffset += targetData.pixelStride;
                }

                sourceLineOffset += sourceData.lineStride;
                targetLineOffset += targetData.lineStride;
            }
        }

        targetAccessor.setPixels(targetData);
    }
}
