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
package org.esa.chris.ui;

import org.esa.chris.operators.ComputeSurfaceReflectancesOp;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dialog for invoking the CHRIS/Proba atmospheric correction.
 */
@ActionID(
        category = "Tools",
        id = "PerformAtmosphericCorrectionAction"
)
@ActionRegistration(
        displayName = "#CTL_PerformAtmosphericCorrectionAction_MenuText",
        popupText = "#CTL_PerformAtmosphericCorrectionAction_ShortDescription"
)
@ActionReference(
        path = "Menu/Optical/CHRIS-Proba Tools",
        position = 303
)
@NbBundle.Messages({
        "CTL_PerformAtmosphericCorrectionAction_MenuText=Atmospheric Correction...",
        "CTL_PerformAtmosphericCorrectionAction_ShortDescription=Calculates surface reflectances for the selected CHRIS/Proba product"
})
public class PerformAtmosphericCorrectionAction extends AbstractSnapAction implements ContextAwareAction {
    private final AtomicReference<ModelessDialog> dialog;

    public PerformAtmosphericCorrectionAction() {
        putValue(Action.NAME, "Atmospheric Correction...");
        putValue(Action.SHORT_DESCRIPTION, "Calculates surface reflectances for the selected CHRIS/Proba product");
        setHelpId("chrisAtmosphericCorrectionTool");
        dialog = new AtomicReference<>();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        Product selectedProduct = getAppContext().getSelectedProduct();
        setEnabled(new AtmosphericCorrectionProductFilter().accept(selectedProduct));
        return this;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog(OperatorSpi.getOperatorAlias(ComputeSurfaceReflectancesOp.class),
                                                     appContext,
                                                     "CHRIS/Proba Atmospheric Correction",
                                                     "chrisAtmosphericCorrectionTool");
        dialog.getJDialog().setName("chrisAtmosphericCorrectionDialog");
        dialog.setTargetProductNameSuffix("_AC");

        return dialog;
    }
}
