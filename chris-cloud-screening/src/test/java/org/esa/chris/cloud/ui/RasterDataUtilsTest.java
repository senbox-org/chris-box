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
package org.esa.chris.cloud.ui;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.chris.cloud.ui.RasterDataUtils;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Assert;
import org.junit.Test;

import javax.media.jai.ComponentSampleModelJAI;
import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

/**
 * Tests for class {@link RasterDataUtils}.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class RasterDataUtilsTest {

    private static final int W = 5;
    private static final int H = 5;

    private static final int TILE_W = 2;
    private static final int TILE_H = 2;

    @Test
    public void testCreateRasterData() {
        final RenderedImage image = createTestImage(1, W, H);
        final ProductData data = RasterDataUtils.createRasterData(image, ProgressMonitor.NULL);

        Assert.assertEquals(H * W, data.getNumElems());
        assertEachElementEquals(1, data);
    }

    private static RenderedImage createTestImage(int value, int imageW, int imageH) {
        return ConstantDescriptor.create((float) imageW, (float) imageH, new Integer[]{value},
                createRenderingHints(imageW, imageH, TILE_W, TILE_H));
    }

    private static RenderingHints createRenderingHints(int imageW, int imageH, int tileW, int tileH) {
        final SampleModel sm = new ComponentSampleModelJAI(DataBuffer.TYPE_INT, imageW, imageH, 1, imageW, new int[]{0});
        final ImageLayout imageLayout = new ImageLayout(0, 0, imageW, imageH, 0, 0, tileW, tileH, sm, PlanarImage.createColorModel(sm));

        final RenderingHints renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE, null);
        renderingHints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout);

        return renderingHints;
    }

    private static void assertEachElementEquals(int expected, ProductData actual) {
        for (int i = 0; i < actual.getNumElems(); ++i) {
            Assert.assertEquals(expected, actual.getElemIntAt(i));
        }
    }
}
