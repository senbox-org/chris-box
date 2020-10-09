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

import org.esa.chris.util.OpUtils;
import org.esa.snap.core.datamodel.Product;
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
 * Action for extracting features needed for cloud screening.
 */
@ActionID(
        category = "Tools",
        id = "ExtractFeaturesAction"
)
@ActionRegistration(
        displayName = "#CTL_ExtractFeaturesAction_MenuText",
        popupText = "#CTL_ExtractFeaturesAction_ShortDescription"
)
@ActionReference(
        path = "Menu/Optical/CHRIS-Proba Tools",
        position = 7
)
@NbBundle.Messages({
        "CTL_ExtractFeaturesAction_MenuText=Feature Extraction...",
        "CTL_ExtractFeaturesAction_ShortDescription=Extracts features from the selected CHRIS/Proba product which are used for cloud screening"
})

public class ExtractFeaturesAction extends AbstractSnapAction implements ContextAwareAction {
    private final AtomicReference<ModelessDialog> dialog;

    public ExtractFeaturesAction() {
        putValue(Action.NAME, Bundle.CTL_ExtractFeaturesAction_MenuText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ExtractFeaturesAction_ShortDescription());
        setHelpId("chrisExtractFeaturesTools");
        dialog = new AtomicReference<>();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        Product selectedProduct = getAppContext().getSelectedProduct();
        boolean enabled = selectedProduct == null ||
                selectedProduct.getProductType().startsWith("CHRIS_M") &&
                        OpUtils.findBands(selectedProduct, "toa_refl").length >= 18;

        setEnabled(enabled);

        return this;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    private ModelessDialog createDialog(AppContext appContext) {
        final DefaultSingleTargetProductDialog dialog =
                new DefaultSingleTargetProductDialog("chris.ExtractFeatures",
                                                     appContext,
                                                     "CHRIS/Proba Feature Extraction",
                                                     getHelpId());
        dialog.setTargetProductNameSuffix("_FEAT");
        return dialog;
    }

}
