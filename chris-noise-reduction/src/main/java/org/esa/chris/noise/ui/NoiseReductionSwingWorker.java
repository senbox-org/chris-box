/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.chris.noise.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.ui.AppContext;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Noise reduction swing worker.
 * <p/>
 * Performs the noise reduction for CHRIS images. Note that each source product
 * is disposed as soon as possible in order to save memory.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
class NoiseReductionSwingWorker extends ProgressMonitorSwingWorker<Object, Product> {

    private final Map<Product, File> sourceProductTargetFileMap;
    private final Product[] destripingFactorsSourceProducts;

    private final Map<String, Object> destripingFactorsParameterMap;
    private final Map<String, Object> dropoutCorrectionParameterMap;

    private final File destripingFactorsTargetFile;
    private final String targetFormatName;

    private final boolean addTargetProductsToAppContext;
    private final AppContext appContext;

    /**
     * Creates a new instance of this class.
     *
     * @param sourceProductTargetFileMap    the mapping of source products onto target
     *                                      files for the destriped and dropout-corrected
     *                                      products.
     * @param destripingFactorsSourceProducts
     *                                      the source products used for calculating
     *                                      the destriping factors.
     * @param destripingFactorsParameterMap the parameter map used for calculating the
     *                                      destriping factors.
     * @param dropoutCorrectionParameterMap the parameter map used for calculating the
     *                                      dropout correction.
     * @param destripingFactorsTargetFile   the target file for storing the destriping
     *                                      factors.
     * @param targetFileFormat              the target fle format.
     * @param appContext                    the application context.
     * @param addTargetProductsToAppContext {@code true} when the target products should
     *                                      be added to the application context.
     */
    public NoiseReductionSwingWorker(Map<Product, File> sourceProductTargetFileMap,
                                     Product[] destripingFactorsSourceProducts,
                                     Map<String, Object> destripingFactorsParameterMap,
                                     Map<String, Object> dropoutCorrectionParameterMap,
                                     File destripingFactorsTargetFile,
                                     String targetFileFormat,
                                     AppContext appContext,
                                     boolean addTargetProductsToAppContext) {
        super(appContext.getApplicationWindow(), "Noise Reduction");

        this.sourceProductTargetFileMap = sourceProductTargetFileMap;
        this.destripingFactorsSourceProducts = destripingFactorsSourceProducts;

        this.destripingFactorsParameterMap = destripingFactorsParameterMap;
        this.dropoutCorrectionParameterMap = dropoutCorrectionParameterMap;

        this.destripingFactorsTargetFile = destripingFactorsTargetFile;
        this.targetFormatName = targetFileFormat;

        this.appContext = appContext;
        this.addTargetProductsToAppContext = addTargetProductsToAppContext;
    }

    @Override
    protected Object doInBackground(ProgressMonitor pm) throws Exception {
        Product destripingFactorsProduct = null;

        try {
            pm.beginTask("Performing noise reduction...", 50 + sourceProductTargetFileMap.size() * 10);
            destripingFactorsProduct = GPF.createProduct("chris.ComputeDestripingFactors",
                                                         destripingFactorsParameterMap,
                                                         destripingFactorsSourceProducts);

            try {
                WriteOp writeOp = new WriteOp(destripingFactorsProduct, destripingFactorsTargetFile, targetFormatName);
                writeOp.writeProduct(new SubProgressMonitor(pm, 50));
            } finally {
                destripingFactorsProduct.dispose();
                for (final Product sourceProduct : destripingFactorsSourceProducts) {
                    if (!sourceProductTargetFileMap.containsKey(sourceProduct)) {
                        disposeSourceProductIfNotUsedInAppContext(sourceProduct);
                    }
                }
            }

            try {
                destripingFactorsProduct = ProductIO.readProduct(destripingFactorsTargetFile);
            } catch (IOException e) {
                throw new OperatorException(MessageFormat.format(
                        "Cannot read file ''{0}''.", destripingFactorsTargetFile), e);
            }

            for (final Map.Entry<Product, File> entry : sourceProductTargetFileMap.entrySet()) {
                final Product sourceProduct = entry.getKey();
                final File targetFile = entry.getValue();

                performNoiseReduction(sourceProduct, destripingFactorsProduct, targetFile,
                                      new SubProgressMonitor(pm, 10));
            }
        } finally {
            if (destripingFactorsProduct != null) {
                destripingFactorsProduct.dispose();
            }
            pm.done();
        }

        return null;
    }

    @Override
    protected void process(List<Product> products) {
        if (addTargetProductsToAppContext) {
            for (Product product : products) {
                appContext.getProductManager().addProduct(product);
            }
        }
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException e) {
            // ignore
        } catch (ExecutionException e) {
            appContext.handleError(e.getMessage(), e.getCause());
        }
    }

    private void performNoiseReduction(Product sourceProduct,
                                       Product destripingFactorsProduct,
                                       File targetFile,
                                       ProgressMonitor pm) throws IOException {
        final HashMap<String, Product> sourceProductMap = new HashMap<>(5);
        sourceProductMap.put("sourceProduct", sourceProduct);
        sourceProductMap.put("factorProduct", destripingFactorsProduct);

        Product destripedProduct = null;

        try {
            destripedProduct = GPF.createProduct("chris.ApplyDestripingFactors",
                                                 new HashMap<>(0),
                                                 sourceProductMap);
            final Product dropoutCorrectedProduct = GPF.createProduct("chris.CorrectDropouts",
                                                                      dropoutCorrectionParameterMap,
                                                                      destripedProduct);

            dropoutCorrectedProduct.setName(FileUtils.getFilenameWithoutExtension(targetFile));
            writeProduct(dropoutCorrectedProduct, targetFile, addTargetProductsToAppContext, pm);
        } finally {
            if (destripedProduct != null) {
                destripedProduct.dispose();
            }
            disposeSourceProductIfNotUsedInAppContext(sourceProduct);
        }
    }

    private void writeProduct(final Product targetProduct, final File targetFile, boolean openInApp,
                              ProgressMonitor pm) throws IOException {
        try {
            pm.beginTask("Writing " + targetProduct.getName() + "...", openInApp ? 100 : 95);

            try {
                WriteOp writeOp = new WriteOp(targetProduct, targetFile, targetFormatName);
                writeOp.setWriteEntireTileRows(true);
                writeOp.writeProduct(new SubProgressMonitor(pm, 50));
            } finally {
                targetProduct.dispose();
            }

            if (openInApp) {
                final Product product = ProductIO.readProduct(targetFile);
                if (product != null) {
                    publish(product);
                }
                pm.worked(5);
            }
        } finally {
            pm.done();
        }
    }

    private void disposeSourceProductIfNotUsedInAppContext(Product sourceProduct) {
        boolean dispose = true;
        for (final Product product : appContext.getProductManager().getProducts()) {
            if (sourceProduct == product) {
                dispose = false;
                break;
            }
        }
        if (dispose) {
            sourceProduct.dispose();
        }
    }
}
