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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for class {@link Roots}.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class RootsTest {

    @Test
    public void testBrent() {
        final UnivariateFunction f = new Functions.Cos();
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 2.0);
        final boolean success = Roots.brent(f, bracket, 100);

        Assert.assertTrue(success);
        Assert.assertEquals(Math.PI / 2.0, bracket.root, 0.0);
    }

    @Test
    public void testBrentWithRootAtBracketingIntervalLowerLimit() {
        final UnivariateFunction f = new Functions.Sin();
        final Roots.Bracket bracket = new Roots.Bracket(0.0, 1.0);
        final boolean success = Roots.brent(f, bracket, 100);

        Assert.assertTrue(success);
        Assert.assertEquals(0.0, bracket.root, 0.0);
    }

    @Test
    public void testBrentWithRootAtBracketingIntervalUpperLimit() {
        final UnivariateFunction f = new Functions.Sin();
        final Roots.Bracket bracket = new Roots.Bracket(-1.0, 0.0);
        final boolean success = Roots.brent(f, bracket, 100);

        Assert.assertTrue(success);
        Assert.assertEquals(0.0, bracket.root, 0.0);
    }

    @Test
    public void testBrentWithRootNotInBracketingInterval() {
        final UnivariateFunction f = new Functions.Cos();

        Roots.Bracket bracket;
        bracket = new Roots.Bracket(0.0, 1.0);

        // the bracketing interval does not bracket a root
        Assert.assertFalse(bracket.isBracket(f));

        boolean success;
        success = Roots.brent(f, bracket, 100);
        // the bracketing interval does not bracket a root, but Brent's
        // algorithm returns the value which is closest to the root
        Assert.assertTrue(success);
        Assert.assertEquals(1.0, bracket.root, 0.0);


        bracket = new Roots.Bracket(Math.PI - 1.0, Math.PI);

        // the bracketing interval does not bracket a root
        Assert.assertFalse(bracket.isBracket(f));

        success = Roots.brent(f, bracket, 100);
        // the bracketing interval does not bracket a root, but Brent's
        // algorithm returns the value which is closest to the root
        Assert.assertTrue(success);
        Assert.assertEquals(Math.PI - 1.0, bracket.root, 0.0);
    }
}
