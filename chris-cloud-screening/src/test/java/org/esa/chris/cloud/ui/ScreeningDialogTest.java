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
package org.esa.chris.cloud.ui;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.DefaultAppContext;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for class {@link ScreeningDialog}.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class ScreeningDialogTest {

    @Before
    public void before() {
        Assume.assumeFalse(GraphicsEnvironment.getLocalGraphicsEnvironment().isHeadlessInstance());
    }

    @Test
    public void sourceProductIsReleasedWhenDialogIsHidden() {
        final Product product = new Product("test", "test", 1, 1);

        ScreeningDialog dialog;
        dialog = new ScreeningDialog(new DefaultAppContext("test"));
        dialog.show();
        dialog.getForm().setSourceProduct(product);
        assertSame(dialog.getForm().getSourceProduct(), dialog.getFormModel().getSourceProduct());

        dialog.hide();
        assertNull(dialog.getFormModel().getSourceProduct());
    }
}
