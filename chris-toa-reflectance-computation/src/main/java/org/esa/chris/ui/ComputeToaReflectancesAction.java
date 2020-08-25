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

import org.esa.chris.util.OpUtils;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for computing TOA reflectances.
 */
@ActionID(
        category = "Tools",
        id = "ComputeToaReflectancesAction"
)
@ActionRegistration(
        displayName = "#CTL_ComputeToaReflectancesAction_MenuText",
        popupText = "#CTL_ComputeToaReflectancesAction_ShortDescription"
)
@ActionReferences({
        @ActionReference(
                path = "Menu/Optical/CHRIS-Proba Tools",
                position = 304
        )
})
@NbBundle.Messages({
        "CTL_ComputeToaReflectancesAction_MenuText=TOA Reflectance Computation...",
        "CTL_ComputeToaReflectancesAction_ShortDescription=Computes TOA reflectances for the selected CHRIS/Proba product"
})
public class ComputeToaReflectancesAction extends AbstractSnapAction implements ContextAwareAction {
    private final AtomicReference<ModelessDialog> dialog;

    public ComputeToaReflectancesAction() {
        putValue(Action.NAME, "Feature Extraction...");
        putValue(Action.SHORT_DESCRIPTION, "Extracts features from the selected CHRIS/Proba product which are used for cloud screening");
        setHelpId("chrisToaReflectanceComputationTool");

        dialog = new AtomicReference<>();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        Product selectedProduct = getAppContext().getSelectedProduct();
        final boolean enabled = selectedProduct == null ||
                selectedProduct.getProductType().startsWith("CHRIS_M") &&
                        OpUtils.findBands(selectedProduct, "radiance").length != 0;

        setEnabled(enabled);

        return this;
    }

    private ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ComputeToaReflectances",
                                                     appContext,
                                                     "CHRIS/Proba TOA Reflectance Computation",
                                                     getHelpId());
        dialog.getJDialog().setName("chrisToaReflectanceComputationDialog");
        dialog.setTargetProductNameSuffix("_TOA_REFL");

        return dialog;
    }
}
