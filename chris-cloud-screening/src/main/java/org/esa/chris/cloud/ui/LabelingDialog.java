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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.netbeans.docwin.DocumentWindowManager;
import org.esa.snap.netbeans.docwin.WindowUtilities;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.window.OpenImageViewAction;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.rcp.windows.ProductSceneViewTopComponent;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.product.ProductSceneView;
import org.openide.awt.UndoRedo;

import javax.swing.AbstractButton;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

/**
 * Cloud labeling form.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 */
class LabelingDialog extends ModelessDialog {

    private static final DocumentWindowManager WINDOW_MANAGER = SnapApp.getDefault().getDocumentWindowManager();
    private final AppContext appContext;
    private final ScreeningContext screeningContext;

    private final LabelingFormModel formModel;
    private final LabelingForm form;

    private final PixelPositionListener pixelPositionListener;

    private final VetoableClosePsvTopComponent colorFrame;
    private final VetoableClosePsvTopComponent classFrame;
    private final DocumentWindowManager.Listener<Object, ProductSceneView> viewListener;

    LabelingDialog(final AppContext appContext, final ScreeningContext screeningContext) {
        super(appContext.getApplicationWindow(),
              MessageFormat.format("CHRIS/Proba Cloud Labeling - {0}", screeningContext.getRadianceProduct().getName()),
              ID_APPLY_CLOSE_HELP, CloudScreeningAction.HELP_ID);

        this.appContext = appContext;
        this.screeningContext = screeningContext;

        formModel = new LabelingFormModel(screeningContext);
        form = new LabelingForm(formModel);

        form.getCheckBox().addActionListener(e -> {
            if (form.getCheckBox().isSelected()) {
                Dialogs.showInformation("CHRIS/Proba Cloud Screening",
                                        "Calculating the probabilistic cloud mask can be extremely time consuming!",
                                        "chrisbox.postLabling.showWarning");
            }
        });

        pixelPositionListener = new PixelPositionListener() {
            @Override
            public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel,
                                        boolean pixelPosValid, MouseEvent e) {
                if (pixelPosValid) {
                    final int classIndex = screeningContext.getClassIndex(pixelX, pixelY, currentLevel);
                    form.getTable().getSelectionModel().setSelectionInterval(classIndex, classIndex);
                }
            }

            @Override
            public void pixelPosNotAvailable() {
            }
        };

        final String radianceProductName = screeningContext.getRadianceProduct().getName();
        final String rgbFrameTitle = MessageFormat.format("{0} - RGB", radianceProductName);
        colorFrame = createTopComponent(screeningContext.getColorView(), rgbFrameTitle);

        final String classFrameTitle = MessageFormat.format("{0} - Classes", radianceProductName);
        classFrame = createTopComponent(screeningContext.getClassView(), classFrameTitle);

        viewListener = new DocumentWindowManager.Listener<Object, ProductSceneView>() {
            @Override
            public void windowClosed(DocumentWindowManager.Event e) {
                if (colorFrame == e.getWindow() || classFrame == e.getWindow()) {
                    LabelingDialog.this.forceClose();
                }
            }
        };
        WINDOW_MANAGER.addListener(viewListener);

        final AbstractButton button = getButton(ID_APPLY);
        button.setText("Run");
        button.setMnemonic('R');
        button.setToolTipText("Creates the cloud mask for the associated product.");
    }

    @Override
    protected void onApply() {
        final Worker worker = new Worker(appContext, screeningContext, formModel);
        worker.execute();
    }

    @Override
    public void hide() {
        form.prepareHide();

        classFrame.setVisible(false);
        colorFrame.setVisible(false);

        super.hide();
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);

        colorFrame.setVisible(true);
        classFrame.setVisible(true);

        return super.show();
    }

    @Override
    protected void onClose() {
        close();
    }

    public void close() {
        forceClose();
    }

    private void forceClose() {
        disposeTopComponent(classFrame);
        disposeTopComponent(colorFrame);

        getJDialog().dispose();
        WINDOW_MANAGER.removeListener(viewListener);
    }

    private VetoableClosePsvTopComponent createTopComponent(ProductSceneView view, String title) {
        view.setNoDataOverlayEnabled(false);
        view.setGraticuleOverlayEnabled(false);
        view.setPinOverlayEnabled(false);
        view.addPixelPositionListener(pixelPositionListener);

        UndoRedo.Manager undoManager = SnapApp.getDefault().getUndoManager(view.getProduct());
        VetoableClosePsvTopComponent psvTopComponent = new VetoableClosePsvTopComponent(view, undoManager);
        psvTopComponent.setDisplayName(title);
        WINDOW_MANAGER.openWindow(psvTopComponent);
        psvTopComponent.requestSelected();


        return psvTopComponent;
    }

    private void disposeTopComponent(VetoableClosePsvTopComponent topComponent) {
        if (topComponent != null && topComponent.isOpened()) {
            topComponent.setVetoAllowed(false);
            final ProductSceneView view = topComponent.getView();
            if (view != null) {
                view.removePixelPositionListener(pixelPositionListener);
                WINDOW_MANAGER.closeWindow(topComponent);
            }
        }
    }

    private static class Worker extends ProgressMonitorSwingWorker<Band, Object> {
        private final AppContext appContext;
        private final ScreeningContext screeningContext;
        private final LabelingFormModel formModel;

        Worker(AppContext appContext, ScreeningContext screeningContext, LabelingFormModel formModel) {
            super(appContext.getApplicationWindow(), "Creating cloud mask...");
            this.appContext = appContext;
            this.screeningContext = screeningContext;
            this.formModel = formModel;
        }

        @Override
        protected Band doInBackground(ProgressMonitor pm) {
            return screeningContext.performCloudMaskCreation(formModel.getCloudyFlags(),
                                                             formModel.getIgnoreFlags(),
                                                             formModel.isProbabilistic(), pm);
        }

        @Override
        protected void done() {
            try {
                final Product radianceProduct = screeningContext.getRadianceProduct();
                final Band newBand = get();
                if (radianceProduct.containsBand(newBand.getName())) {
                    final Band oldBand = radianceProduct.getBand(newBand.getName());
                    ProductSceneViewTopComponent psvTopComponent = getProductSceneViewTopComponent(oldBand);
                    WINDOW_MANAGER.closeWindow(psvTopComponent);
                    radianceProduct.removeBand(oldBand);
                }
                radianceProduct.addBand(newBand);
                OpenImageViewAction.openImageView(newBand);
            } catch (InterruptedException e) {
                appContext.handleError(e.getMessage(), e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause().getMessage(), e.getCause());
            }
        }
    }

    private static ProductSceneViewTopComponent getProductSceneViewTopComponent(RasterDataNode raster) {
        return WindowUtilities.getOpened(ProductSceneViewTopComponent.class)
                .filter(topComponent -> raster == topComponent.getView().getRaster())
                .findFirst()
                .orElse(null);
    }

}