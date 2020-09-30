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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.ui.TargetProductSelectorModel;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.actions.AbstractSnapAction;
import org.esa.snap.rcp.actions.file.SaveProductAsAction;
import org.esa.snap.ui.ModalDialog;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;

import javax.swing.Action;
import javax.swing.JOptionPane;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action for invoking the CHRIS/Proba noise reduction.
 *
 * @author Marco Peters
 * @author Ralf Quast
 */
@ActionID(
        category = "Tools",
        id = "NoiseReductionAction"
)
@ActionRegistration(
        displayName = "Noise Reduction...",
        popupText = "Performs the noise reduction for the selected CHRIS/Proba product"
)
@ActionReference(
        path = "Menu/Optical/CHRIS-Proba Tools",
        position = 1
)

public class NoiseReductionAction extends AbstractSnapAction implements ContextAwareAction {

    static final String SOURCE_NAME_PATTERN = "${sourceName}";

    private static final String DIALOG_TITLE = "CHRIS/Proba Noise Reduction";
    private static final String SOURCE_NAME_REGEX = "\\$\\{sourceName}";

    public NoiseReductionAction() {
        putValue(Action.NAME, "Noise Reduction...");
        putValue(Action.SHORT_DESCRIPTION, "Performs the noise reduction for the selected CHRIS/Proba product");
        setHelpId("chrisNoiseReductionTool");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Product[] acquisitionSet = new AcquisitionSetProvider().getAcquisitionSet(getAppContext());

        final NoiseReductionPresenter presenter = new NoiseReductionPresenter(getAppContext(),
                                                                              acquisitionSet,
                                                                              new AdvancedSettingsPresenter());
        final NoiseReductionForm noiseReductionForm = new NoiseReductionForm(presenter);
        noiseReductionForm.getTargetProductSelector().getOpenInAppCheckBox().setText(
                "Open in " + getAppContext().getApplicationName());

        final TargetProductSelectorModel targetProductSelectorModel = noiseReductionForm.getTargetProductSelectorModel();
        final ModalDialog dialog =
                new ModalDialog(SnapApp.getDefault().getMainFrame(), DIALOG_TITLE, ModalDialog.ID_OK_CANCEL_HELP,
                                "chrisNoiseReductionTool") {
                    @Override
                    protected boolean verifyUserInput() {
                        if (!targetProductSelectorModel.getProductName().contains(SOURCE_NAME_PATTERN)) {
                            showErrorDialog(
                                    "Target product name must use the '" + SOURCE_NAME_PATTERN + "' expression.");
                            return false;
                        }

                        final Product[] sourceProducts = presenter.getSourceProducts();
                        if (sourceProducts.length == 0) {
                            showWarningDialog("At least one product must be selected for noise reduction.");
                            return false;
                        }
                        final File[] targetProductFiles = new File[sourceProducts.length];
                        List<String> existingFilePathList = new ArrayList<>(7);
                        for (int i = 0; i < sourceProducts.length; ++i) {
                            targetProductFiles[i] = createResolvedTargetFile(
                                    targetProductSelectorModel.getProductFile(), sourceProducts[i].getName());
                            if (targetProductFiles[i].exists()) {
                                existingFilePathList.add(targetProductFiles[i].getAbsolutePath());
                            }
                        }
                        if (!existingFilePathList.isEmpty()) {
                            String fileList = StringUtils.arrayToString(
                                    existingFilePathList.toArray(new String[0]), "\n");
                            String message = "The specified output file(s)\n{0}\nalready exists.\n\n" +
                                    "Do you want to overwrite the existing file(s)?";
                            String formatedMessage = MessageFormat.format(message, fileList);
                            final int answer = JOptionPane.showConfirmDialog(this.getJDialog(), formatedMessage,
                                                                             DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
                            return answer == JOptionPane.YES_OPTION;
                        }

                        return true;
                    }
                };
        dialog.getJDialog().setName("chrisNoiseReductionDialog");
        dialog.setContent(noiseReductionForm);

        final String homeDirPath = SystemUtils.getUserHomeDir().getPath();
        final PropertyMap preferences = getAppContext().getPreferences();
        final String saveDirPath = preferences.getPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, homeDirPath);
        targetProductSelectorModel.setProductDir(new File(saveDirPath));
        targetProductSelectorModel.setProductName(SOURCE_NAME_PATTERN + "_NR");

        if (dialog.show() == ModalDialog.ID_OK) {
            performNoiseReduction(presenter, targetProductSelectorModel);
        } else {
            for (final Product product : presenter.getDestripingFactorsSourceProducts()) {
                if (!getAppContext().getProductManager().contains(product)) {
                    product.dispose();
                }
            }
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup actionContext) {
        Product selectedProduct = getAppContext().getSelectedProduct();
        setEnabled(new NoiseReductionProductFilter().accept(selectedProduct));
        return this;
    }

    private void performNoiseReduction(NoiseReductionPresenter presenter,
                                       TargetProductSelectorModel targetProductSelectorModel) {
        final String productDirPath = targetProductSelectorModel.getProductDir().getAbsolutePath();
        final PropertyMap preferences = getAppContext().getPreferences();
        preferences.setPropertyString(SaveProductAsAction.PREFERENCES_KEY_LAST_PRODUCT_DIR, productDirPath);

        final Map<Product, File> sourceProductTargetFileMap = new HashMap<>(7);
        for (final Product sourceProduct : presenter.getSourceProducts()) {
            final File unresolvedTargetFile = targetProductSelectorModel.getProductFile();
            final File targetFile = createResolvedTargetFile(unresolvedTargetFile, sourceProduct.getName());

            sourceProductTargetFileMap.put(sourceProduct, targetFile);
        }

        final NoiseReductionSwingWorker worker = new NoiseReductionSwingWorker(
                sourceProductTargetFileMap,
                presenter.getDestripingFactorsSourceProducts(),
                presenter.getDestripingFactorsParameterMap(),
                presenter.getDropoutCorrectionParameterMap(),
                createDestripingFactorsTargetFile(sourceProductTargetFileMap.values().iterator().next()),
                targetProductSelectorModel.getFormatName(),
                getAppContext(),
                targetProductSelectorModel.isOpenInAppSelected());

        worker.execute();
    }

    private File createResolvedTargetFile(File unresolvedTargetFile, String sourceProductName) {
        final String unresolvedFileName = unresolvedTargetFile.getName();
        final String resolvedFileName = unresolvedFileName.replaceAll(SOURCE_NAME_REGEX, sourceProductName);

        return new File(unresolvedTargetFile.getParentFile(), resolvedFileName);
    }


    private File createDestripingFactorsTargetFile(File targetFile) {
        final String basename = FileUtils.getFilenameWithoutExtension(targetFile);
        final String extension = FileUtils.getExtension(targetFile);

        return new File(targetFile.getParentFile(), basename + "_VSC" + extension);
    }
}
