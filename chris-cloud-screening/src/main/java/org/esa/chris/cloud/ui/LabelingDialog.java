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
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.util.Dialogs;
import org.esa.snap.ui.AppContext;
import org.esa.snap.ui.ModelessDialog;
import org.esa.snap.ui.PixelPositionListener;
import org.esa.snap.ui.UIUtils;
import org.esa.snap.ui.product.ProductSceneView;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JInternalFrame;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;

/**
 * Cloud labeling form.
 *
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 */
class LabelingDialog extends ModelessDialog {

    private final AppContext appContext;
    private final ScreeningContext screeningContext;

    private final LabelingFormModel formModel;
    private final LabelingForm form;

    private final PixelPositionListener pixelPositionListener;
    private final VetoableChangeListener frameClosedListener;

    private final JInternalFrame colorFrame;
    private final JInternalFrame classFrame;

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

        frameClosedListener = evt -> {
            if (JInternalFrame.IS_CLOSED_PROPERTY.equals(evt.getPropertyName())) {
                if ((Boolean) evt.getNewValue()) {
                    final Dialogs.Answer answer = Dialogs.requestDecision("Question",
                                                               "All windows associated with the cloud labeling dialog will be closed. Do you really want to close the cloud labeling dialog?",
                                                               false, null);
                    if (answer == Dialogs.Answer.YES) {
                        close();
                    } else {
                        throw new PropertyVetoException("Do not close.", evt);
                    }
                }
            }
        };

        final String radianceProductName = screeningContext.getRadianceProduct().getName();
        final String rgbFrameTitle = MessageFormat.format("{0} - RGB", radianceProductName);
        colorFrame = createInternalFrame(screeningContext.getColorView(), rgbFrameTitle);

        final String classFrameTitle = MessageFormat.format("{0} - Classes", radianceProductName);
        classFrame = createInternalFrame(screeningContext.getClassView(), classFrameTitle);

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

        classFrame.hide();
        colorFrame.hide();

        super.hide();
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);

        colorFrame.show();
        classFrame.show();

        return super.show();
    }

    @Override
    protected void onClose() {
        close();
    }

    @Override
    public void close() {
        disposeInternalFrame(classFrame);
        disposeInternalFrame(colorFrame);

        getJDialog().dispose();
    }

    private JInternalFrame createInternalFrame(ProductSceneView view, String title) {
        final SnapApp snapApp = SnapApp.getDefault();
        // TODO - fix this
//        view.setCommandUIFactory(snapApp.getCommandUIFactory());
        view.setNoDataOverlayEnabled(false);
        view.setGraticuleOverlayEnabled(false);
        view.setPinOverlayEnabled(false);
//        view.setLayerProperties(snapApp.getPreferences());
        view.addPixelPositionListener(pixelPositionListener);

        final Icon icon = UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif");

//        final JInternalFrame frame = snapApp.getDocumentWindowManager(title, icon, view, "");

        final JInternalFrame frame = new JInternalFrame(title);
        frame.addVetoableChangeListener(frameClosedListener);

        return frame;
    }

    private void disposeInternalFrame(JInternalFrame frame) {
        if (frame != null && !frame.isClosed()) {
            frame.removeVetoableChangeListener(frameClosedListener);

            final Container contentPane = frame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                view.removePixelPositionListener(pixelPositionListener);
                // todo - fix this
                // VisatApp.getApp().getDesktopPane().closeFrame(frame);
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
                    // todo - fix this
//                    final JInternalFrame oldFrame = VisatApp.getApp().findInternalFrame(oldBand);
//                    if (oldFrame != null) {
//                        VisatApp.getApp().getDesktopPane().closeFrame(oldFrame);
//                    }
                    radianceProduct.removeBand(oldBand);
                }
                radianceProduct.addBand(newBand);
                // todo - fix this
//                VisatApp.getApp().openProductSceneView(newBand);
            } catch (InterruptedException e) {
                appContext.handleError(e.getMessage(), e);
            } catch (ExecutionException e) {
                appContext.handleError(e.getCause().getMessage(), e.getCause());
            }
        }
    }
}