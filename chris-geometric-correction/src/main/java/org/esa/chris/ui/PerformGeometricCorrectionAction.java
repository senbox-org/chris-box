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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.chris.operators.PerformGeometricCorrectionOp;
import org.esa.chris.operators.TimeConverter;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.ModelessDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

import javax.swing.Action;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Action for invoking the geometric correction.
 */
@ActionID(
        category = "Tools",
        id = "PerformGeometricCorrectionAction"
)
@ActionRegistration(
        displayName = "#CTL_PerformGeometricCorrectionAction_MenuText",
        popupText = "#CTL_PerformGeometricCorrectionAction_ShortDescription"
)
        @ActionReference(
                path = "Menu/Optical/CHRIS-Proba Tools",
                position = 4
        )
@NbBundle.Messages({
        "CTL_PerformGeometricCorrectionAction_MenuText=Geometric Correction...",
        "CTL_PerformGeometricCorrectionAction_ShortDescription=Performs the geometric correction for the selected CHRIS/Proba product"
})
public class PerformGeometricCorrectionAction extends AbstractSnapAction implements ContextAwareAction {

    private static final String TITLE = "CHRIS/Proba Geometric Correction";
    private static final String KEY_FETCH_LATEST_TIME_TABLES = "chris.geoCorrection.fetchLatestTimeTables";
    private static final String QUESTION_FETCH_LATEST_TIME_TABLES =
            "Your UT1 and leap second time tables are older than 7 days. Fetching\n" +
            "the latest time tables from the web can take a few minutes.\n" +
            "\n" +
            "Do you want to fetch the latest time tables now?";

    private final AtomicReference<ModelessDialog> dialog;

    public PerformGeometricCorrectionAction() {
        putValue(Action.NAME, "Geometric Correction...");
        putValue(Action.SHORT_DESCRIPTION, "Performs the geometric correction for the selected CHRIS/Proba product");
        setHelpId("chrisGeometricCorrectionTool");
        dialog = new AtomicReference<>();
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        final Product selectedProduct = getAppContext().getSelectedProduct();
        setEnabled(new GeometricCorrectionProductFilter().accept(selectedProduct));
        return this;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final TimeConverter converter;

        try {
            converter = TimeConverter.getInstance();
        } catch (IOException ioe) {
            getAppContext().handleError("The geometric correction cannot be carried out because an error occurred.", ioe);
            return;
        }
        if (converter.isOutdated()) {
            final Dialogs.Answer answer = Dialogs.requestDecision(TITLE, QUESTION_FETCH_LATEST_TIME_TABLES,
                                                                  false, KEY_FETCH_LATEST_TIME_TABLES);
            if (Dialogs.Answer.YES.equals(answer)) {
                final Window applicationWindow = getAppContext().getApplicationWindow();
                final TimeConverterUpdater updater = new TimeConverterUpdater(applicationWindow, TITLE, converter);
                updater.execute();
                // dialog is created and shown when updater is done
            } else {
                dialog.compareAndSet(null, createDialog());
                dialog.get().show();
            }
        } else {
            dialog.compareAndSet(null, createDialog());
            dialog.get().show();
        }
    }

    private ModelessDialog createDialog() {
        final String operatorAlias = OperatorSpi.getOperatorAlias(PerformGeometricCorrectionOp.class);
        return new GeometricCorrectionDialog(operatorAlias, getAppContext(), TITLE, getHelpId());
    }


    private class TimeConverterUpdater extends ProgressMonitorSwingWorker<Object, Object> {

        private final TimeConverter converter;

        public TimeConverterUpdater(Window applicationWindow, String title, TimeConverter converter) {
            super(applicationWindow, title);
            this.converter = converter;
        }

        @Override
        protected Object doInBackground(ProgressMonitor pm) throws Exception {
            converter.updateTimeTables(pm);
            return null;
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                getAppContext().handleError("An error occurred while updating the UT1 and leap second time tables.", e);
                return;
            } catch (ExecutionException e) {
                getAppContext().handleError("An error occurred while updating the UT1 and leap second time tables.",
                                            e.getCause());
                return;
            }
            Dialogs.showInformation(TITLE, "The UT1 and leap second time tables have been updated successfully.", null);
            dialog.compareAndSet(null, createDialog());
            dialog.get().show();
        }
    }

}
