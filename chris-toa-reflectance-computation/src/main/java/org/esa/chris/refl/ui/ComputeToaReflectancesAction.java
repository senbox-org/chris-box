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

package org.esa.chris.refl.ui;

import org.esa.chris.util.OpUtils;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.DefaultSingleTargetProductDialog;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collection;
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
@ActionReference(
        path = "Menu/Optical/CHRIS-Proba Tools",
        position = 6,
        separatorBefore = 5
)
@NbBundle.Messages({
        "CTL_ComputeToaReflectancesAction_MenuText=TOA Reflectance Computation...",
        "CTL_ComputeToaReflectancesAction_ShortDescription=Computes TOA reflectances for the selected CHRIS/Proba product"
})
public class ComputeToaReflectancesAction extends AbstractSnapAction implements LookupListener {

    private final AtomicReference<ModelessDialog> dialog;
    private final Lookup.Result<Product> lookupResult;

    public ComputeToaReflectancesAction() {
        putValue(Action.NAME, Bundle.CTL_ComputeToaReflectancesAction_MenuText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_ComputeToaReflectancesAction_ShortDescription());
        setHelpId("chrisToaReflectanceComputationTool");
        dialog = new AtomicReference<>();

        Lookup lookup = Utilities.actionsGlobalContext();
        lookupResult = lookup.lookupResult(Product.class);
        lookupResult.addLookupListener(WeakListeners.create(LookupListener.class, this, lookupResult));
        setEnabled(false);
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Collection<? extends Product> products = lookupResult.allInstances();
        boolean enable = false;
        for (Product product : products) {
            enable = enable || (product.getProductType().startsWith("CHRIS_M") &&
                                OpUtils.findBands(product, "radiance").length != 0);

        }
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
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
