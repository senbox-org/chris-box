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

package org.esa.chris.dataio.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests for class {@link MaskRefinement}.
 *
 * @author Ralf Quast
 */
public class MaskRefinementTest {

    private final static int ROW_COUNT = 2;
    private final static int COL_COUNT = 10;

    @Test
    public void testMaskRefinement() {
        final int[] data = new int[ROW_COUNT * COL_COUNT];
        final short[] mask = new short[ROW_COUNT * COL_COUNT];

        for (int i = 0; i < ROW_COUNT; ++i) {
            for (int j = 0; j < COL_COUNT; ++j) {
                data[i * COL_COUNT + j] = 1;
            }
        }

        final MaskRefinement maskRefinement = new MaskRefinement(1.5);
        maskRefinement.refine(data, mask, COL_COUNT);

        assertEquals(0, mask[0]);
        assertEquals(0, mask[1]);
        assertEquals(0, mask[2]);
        assertEquals(0, mask[3]);
        assertEquals(0, mask[4]);
        assertEquals(0, mask[5]);
        assertEquals(0, mask[6]);
        assertEquals(0, mask[7]);
        assertEquals(0, mask[8]);
        assertEquals(0, mask[9]);

        assertEquals(0, mask[10]);
        assertEquals(0, mask[11]);
        assertEquals(0, mask[12]);
        assertEquals(0, mask[13]);
        assertEquals(0, mask[14]);
        assertEquals(0, mask[15]);
        assertEquals(0, mask[16]);
        assertEquals(0, mask[17]);
        assertEquals(0, mask[18]);
        assertEquals(0, mask[19]);

        for (int i = 0; i < ROW_COUNT; ++i) {
            for (int j = 0; j < COL_COUNT; ++j) {
                if ((j % 2) == 0) {
                    data[i * COL_COUNT + j] = 10;
                }
            }
        }

        maskRefinement.refine(data, mask, COL_COUNT);

        assertEquals(1, mask[0]);
        assertEquals(0, mask[1]);
        assertEquals(1, mask[2]);
        assertEquals(0, mask[3]);
        assertEquals(1, mask[4]);
        assertEquals(0, mask[5]);
        assertEquals(1, mask[6]);
        assertEquals(0, mask[7]);
        assertEquals(1, mask[8]);
        assertEquals(0, mask[9]);

        assertEquals(1, mask[10]);
        assertEquals(0, mask[11]);
        assertEquals(1, mask[12]);
        assertEquals(0, mask[13]);
        assertEquals(1, mask[14]);
        assertEquals(0, mask[15]);
        assertEquals(1, mask[16]);
        assertEquals(0, mask[17]);
        assertEquals(1, mask[18]);
        assertEquals(0, mask[19]);
    }

    @Test
    public void testAdjacentDifference() {
        final int[] values = new int[]{2, 3, 5, 7, 11, 13, 17, 19};
        final double[] diffs = new double[2];

        MaskRefinement.adjacentDifference(values, 0, 1, diffs);
        assertEquals(1, diffs[0], 0.0);
        assertEquals(2, diffs[1], 0.0);

        MaskRefinement.adjacentDifference(values, 2, 1, diffs);
        assertEquals(2, diffs[0], 0.0);
        assertEquals(4, diffs[1], 0.0);

        MaskRefinement.adjacentDifference(values, 0, 2, diffs);
        assertEquals(3, diffs[0], 0.0);
        assertEquals(6, diffs[1], 0.0);

        MaskRefinement.adjacentDifference(values, 1, 2, diffs);
        assertEquals(4, diffs[0], 0.0);
        assertEquals(6, diffs[1], 0.0);

        MaskRefinement.adjacentDifference(values, 0, 3, diffs);
        assertEquals(5, diffs[0], 0.0);
        assertEquals(10, diffs[1], 0.0);

        MaskRefinement.adjacentDifference(values, 1, 3, diffs);
        assertEquals(8, diffs[0], 0.0);
        assertEquals(8, diffs[1], 0.0);
    }


    @Test
    public void testMedian() {
        try {
            MaskRefinement.median(null);
            fail();
        } catch (NullPointerException ignored) {
        }

        try {
            MaskRefinement.median(new double[0]);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        assertEquals(1.0, MaskRefinement.median(new double[]{1.0}), 0.0);
        assertEquals(2.0, MaskRefinement.median(new double[]{2.0, 1.0, 3.0}), 0.0);
        assertEquals(3.0, MaskRefinement.median(new double[]{3.0, 1.0, 4.0, 2.0, 5.0}), 0.0);
    }

}
