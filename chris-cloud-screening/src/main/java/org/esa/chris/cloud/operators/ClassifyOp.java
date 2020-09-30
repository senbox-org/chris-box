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
package org.esa.chris.cloud.operators;

import org.esa.chris.cloud.operators.lut.ClassOpImage;
import org.esa.snap.cluster.EMCluster;
import org.esa.snap.cluster.IndexFilter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

/**
 * Operator for creating a classification product.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
@OperatorMetadata(alias = "chris.Classify",
                  version = "1.0",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008 by Brockmann Consult",
                  description = "Classifies features extracted from TOA reflectances.",
                  internal = true)
public class ClassifyOp extends Operator {

    private static final IndexFilter NO_FILTERING = index -> true;

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String[] sourceBandNames;
    @Parameter
    private EMCluster[] clusters;

    @Override
    public void initialize() throws OperatorException {
        try {
            final int w = sourceProduct.getSceneRasterWidth();
            final int h = sourceProduct.getSceneRasterHeight();

            targetProduct = new Product(sourceProduct.getName() + "_CLASS",
                                        sourceProduct.getProductType() + "_CLASS", w, h);

            final Band classBand = new Band("class_indices", ProductData.TYPE_UINT8, w, h);
            classBand.setDescription("Class indices");
            targetProduct.addBand(classBand);

            final IndexCoding indexCoding = new IndexCoding("Class indices");
            for (int i = 0; i < clusters.length; i++) {
                indexCoding.addIndex("class_" + (i + 1), i, "Class label");
            }
            targetProduct.getIndexCodingGroup().add(indexCoding);
            classBand.setSampleCoding(indexCoding);
            classBand.setSourceImage(ClassOpImage.createImage(sourceProduct, sourceBandNames, clusters, NO_FILTERING));
        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ClassifyOp.class);
        }
    }
}
