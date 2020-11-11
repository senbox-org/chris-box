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
 * Action for invoking the CHIRS/Proba cloud screening dialog.
 */
@ActionID(
        category = "Tools",
        id = "CloudScreeningAction"
)
@ActionRegistration(
        displayName = "#CTL_CloudScreeningAction_MenuText",
        popupText = "#CTL_CloudScreeningAction_ShortDescription"
)
@ActionReference(
        path = "Menu/Optical/CHRIS-Proba Tools",
        position = 2
)
@NbBundle.Messages({
        "CTL_CloudScreeningAction_MenuText=Cloud Screening...",
        "CTL_CloudScreeningAction_ShortDescription=Calculates the cloud mask for the selected CHRIS/Proba product"
})
public class CloudScreeningAction extends AbstractSnapAction implements LookupListener {

    private final AtomicReference<ModelessDialog> dialog;
    private final Lookup.Result<Product> lookupResult;

    public CloudScreeningAction() {
        putValue(Action.NAME, Bundle.CTL_CloudScreeningAction_MenuText());
        putValue(Action.SHORT_DESCRIPTION, Bundle.CTL_CloudScreeningAction_ShortDescription());
        setHelpId("chrisCloudScreeningTools");
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
        CloudScreeningProductFilter productFilter = new CloudScreeningProductFilter();
        for (Product product : products) {
            enable = enable || productFilter.accept(product);
        }
        setEnabled(enable);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.compareAndSet(null, createDialog(getAppContext()));
        dialog.get().show();
    }

    private static ModelessDialog createDialog(AppContext appContext) {
        final ModelessDialog dialog = new ScreeningDialog(appContext);
        dialog.getJDialog().setName("chrisCloudScreeningDialog");
        return dialog;
    }

}
