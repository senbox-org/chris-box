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

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;

import javax.swing.AbstractButton;
import java.util.concurrent.ExecutionException;

/**
 * Cloud screening dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.5
 */
class ScreeningDialog extends ModelessDialog {

    private final AppContext appContext;
    private final ScreeningFormModel formModel;
    private final ScreeningForm form;

    ScreeningDialog(AppContext appContext) {
        super(appContext.getApplicationWindow(), "CHRIS/Proba Cloud Screening", ID_APPLY_CLOSE_HELP,
              CloudScreeningAction.HELP_ID);

        this.appContext = appContext;
        formModel = new ScreeningFormModel();
        form = new ScreeningForm(appContext, formModel);

        final AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        button.setToolTipText("Performs an EM cluster analysis on the selected product.");
    }

    @Override
    protected void onApply() {
        final Product sourceProduct = formModel.getSourceProduct();

        if (!appContext.getProductManager().contains(sourceProduct)) {
            appContext.getProductManager().addProduct(sourceProduct);
        }

        final Worker worker = new Worker(appContext, formModel);
        worker.execute();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    ScreeningForm getForm() {
        return form;
    }

    ScreeningFormModel getFormModel() {
        return formModel;
    }

    private static class Worker extends ProgressMonitorSwingWorker<ScreeningContext, Object> {
        private final AppContext appContext;
        private final ScreeningFormModel formModel;

        private Worker(AppContext appContext, ScreeningFormModel formModel) {
            super(appContext.getApplicationWindow(), "Performing Cluster Analysis...");

            this.appContext = appContext;
            this.formModel = formModel;
        }

        @Override
        protected ScreeningContext doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
            return new ScreeningContext(formModel, appContext.getPreferences(), pm);
        }

        @Override
        protected void done() {
            try {
                final ScreeningContext context = get();
                final ModelessDialog dialog = new LabelingDialog(appContext, context);
                dialog.show();
            } catch (InterruptedException e) {
                appContext.handleError(e.getMessage(), e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause().getMessage(), e.getCause());
            }
        }
    }
}
