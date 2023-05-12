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
package org.esa.chris.cloud.operators;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.chris.cloud.operators.lut.Clusterer;
import org.esa.chris.cloud.operators.lut.ExclusiveIndexFilter;
import org.esa.chris.cloud.operators.lut.InclusiveIndexFilter;
import org.esa.chris.cloud.operators.lut.PixelAccessor;
import org.esa.chris.util.BandFilter;
import org.esa.chris.util.OpUtils;
import org.esa.snap.cluster.EMCluster;
import org.esa.snap.cluster.IndexFilter;
import org.esa.snap.cluster.ProbabilityCalculator;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;
import eu.esa.opt.unmixing.Endmember;

import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Extracts endmembers for calculating cloud abundances.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "chris.ExtractEndmembers",
                  version = "1.1",
                  authors = "Ralf Quast, Marco Zühlke",
                  copyright = "(c) 2008-2020 Brockmann Consult",
                  description = "Finds endmembers for calculating cloud abundances.",
                  internal = true)
public class ExtractEndmembersOp extends Operator {

    public static final BandFilter BAND_FILTER = new ExclusiveMultiBandFilter(new double[][]{
            {400.0, 440.0},
            {590.0, 600.0},
            {630.0, 636.0},
            {648.0, 658.0},
            {686.0, 709.0},
            {792.0, 799.0},
            {756.0, 775.0},
            {808.0, 840.0},
            {885.0, 985.0},
            {985.0, 1010.0}});

    @SourceProduct(alias = "reflectances")
    private Product reflectanceProduct;
    @SourceProduct(alias = "features")
    private Product featureProduct;
    @SourceProduct(alias = "classification")
    private Product classificationProduct;

    @Parameter
    private String[] featureBandNames;
    @Parameter
    private EMCluster[] clusters;
    @Parameter
    private boolean[] cloudFlags;
    @Parameter
    private boolean[] invalidFlags;

    @TargetProperty
    private Endmember[] endmembers;

    /**
     * Constructs a new instance of this class.
     */
    public ExtractEndmembersOp() {
    }

    ExtractEndmembersOp(Product reflectanceProduct,
                                Product featureProduct,
                                Product classificationProduct,
                                String[] featureBandNames,
                                EMCluster[] clusters,
                                boolean[] cloudFlags,
                                boolean[] invalidFlags) {
        this.reflectanceProduct = reflectanceProduct;
        this.featureProduct = featureProduct;
        this.classificationProduct = classificationProduct;
        this.featureBandNames = featureBandNames;
        this.clusters = clusters;
        this.cloudFlags = cloudFlags;
        this.invalidFlags = invalidFlags;
    }

    @Override
    public void initialize() throws OperatorException {
        endmembers = extractEndmembers(reflectanceProduct,
                                       featureProduct,
                                       classificationProduct,
                                       featureBandNames,
                                       clusters,
                                       cloudFlags,
                                       invalidFlags,
                                       ProgressMonitor.NULL);
        setTargetProduct(new Product("EMPTY", "EMPTY_TYPE", 0, 0));
    }

    public static Endmember[] extractEndmembers(Product reflectanceProduct,
                                                Product featureProduct,
                                                Product classificationProduct,
                                                String[] featureBandNames,
                                                EMCluster[] clusters,
                                                final boolean[] cloudFlags,
                                                final boolean[] invalidFlags, ProgressMonitor pm) {
        try {
            pm.beginTask("Extracting endmembers...", 100);

            final ExtractEndmembersOp op = new ExtractEndmembersOp(reflectanceProduct,
                                                                   featureProduct,
                                                                   classificationProduct,
                                                                   featureBandNames,
                                                                   clusters,
                                                                   cloudFlags,
                                                                   invalidFlags);
            final Band[] reflectanceBands = OpUtils.findBands(reflectanceProduct, "toa_refl", BAND_FILTER);
            final double[] wavelengths = OpUtils.getWavelenghts(reflectanceBands);

            final PixelAccessor featAccessor = createPixelAccessor(op, featureProduct, featureBandNames);
            final PixelAccessor reflAccessor = createPixelAccessor(op, reflectanceBands);

            final IndexFilter validFilter = new ExclusiveIndexFilter(invalidFlags);
            final IndexFilter cloudFilter = new InclusiveIndexFilter(cloudFlags);
            final IndexFilter earthFilter = new ExclusiveIndexFilter(cloudFlags, invalidFlags);

            final double[] cloudReflectances = extractCloudReflectances(op, featAccessor,
                                                                        reflAccessor,
                                                                        clusters,
                                                                        cloudFilter,
                                                                        validFilter,
                                                                        SubProgressMonitor.create(pm, 40));

            final double[][] surfaceReflectances = extractSurfaceReflectances(op, featAccessor,
                                                                              reflAccessor,
                                                                              clusters,
                                                                              earthFilter,
                                                                              validFilter,
                                                                              SubProgressMonitor.create(pm, 60));

            final ArrayList<Endmember> endmemberList = new ArrayList<>();
            endmemberList.add(new Endmember("cloud", wavelengths, cloudReflectances));

            final SampleCoding sampleCoding = classificationProduct.getBand("class_indices").getSampleCoding();
            for (int k = 0; k < clusters.length; ++k) {
                if (earthFilter.accept(k)) {
                    final String name = sampleCoding.getSampleName(k);
                    endmemberList.add(new Endmember(name, wavelengths, surfaceReflectances[k]));
                }
            }
            return endmemberList.toArray(new Endmember[0]);
        } finally {
            pm.done();
        }
    }

    private static double[] extractCloudReflectances(ExtractEndmembersOp op,
                                                     PixelAccessor featAccessor,
                                                     PixelAccessor reflAccessor,
                                                     EMCluster[] clusters,
                                                     IndexFilter cloudFilter,
                                                     IndexFilter validFilter,
                                                     ProgressMonitor pm) {
        try {
            pm.beginTask("Extracting cloud endmember", featAccessor.getPixelCount() / 500);

            final ProbabilityCalculator calculator = Clusterer.createProbabilityCalculator(clusters);

            int maxIndex = -1;
            double maxRatio = 0.0;

            for (int i = 0; i < featAccessor.getPixelCount(); ++i) {
                final double[] features = new double[featAccessor.getSampleCount()];
                featAccessor.getSamples(i, features);

                if (features[1] > 0.0) {
                    final double[] posteriors = new double[clusters.length];
                    calculator.calculate(features, posteriors, validFilter);

                    for (int k = 0; k < clusters.length; ++k) {
                        if (posteriors[k] > 0.5) {
                            if (cloudFilter.accept(k)) {
                                final double ratio = features[0] / features[1];

                                if (maxIndex == -1 || ratio > maxRatio) {
                                    maxIndex = i;
                                    maxRatio = ratio;
                                }
                            }
                        }
                    }
                }
                if (i % 500 == 0) {
                    op.checkForCancellation();
                    pm.worked(1);
                }
            }
            return reflAccessor.getSamples(maxIndex, new double[reflAccessor.getSampleCount()]);
        } finally {
            pm.done();
        }
    }

    private static double[][] extractSurfaceReflectances(ExtractEndmembersOp op,
                                                         PixelAccessor featAccessor,
                                                         PixelAccessor reflAccessor,
                                                         EMCluster[] clusters,
                                                         IndexFilter earthFilter,
                                                         IndexFilter validFilter,
                                                         ProgressMonitor pm) {
        try {
            pm.beginTask("Extracting surface endmembers", featAccessor.getPixelCount() / 500);

            final ProbabilityCalculator calculator = Clusterer.createProbabilityCalculator(clusters);

            final double[][] reflectances = new double[clusters.length][reflAccessor.getSampleCount()];
            final int[] count = new int[clusters.length];

            for (int i = 0; i < featAccessor.getPixelCount(); ++i) {
                final double[] features = new double[featAccessor.getSampleCount()];
                featAccessor.getSamples(i, features);
                final double[] posteriors = new double[clusters.length];
                calculator.calculate(features, posteriors, validFilter);

                for (int k = 0; k < clusters.length; ++k) {
                    if (posteriors[k] > 0.5) {
                        if (earthFilter.accept(k)) {
                            reflAccessor.addSamples(i, reflectances[k]);
                            ++count[k];
                            break;
                        }
                    }
                }
                if (i % 500 == 0) {
                    op.checkForCancellation();
                    pm.worked(1);
                }
            }
            for (int k = 0; k < clusters.length; ++k) {
                int countValue = count[k];
                if (countValue > 0) {
                    double[] reflectance = reflectances[k];
                    for (int i = 0; i < reflAccessor.getSampleCount(); ++i) {
                        reflectance[i] /= countValue;
                    }
                }
            }
            return reflectances;
        } finally {
            pm.done();
        }
    }

    private static PixelAccessor createPixelAccessor(Operator op, Band[] bands) {
        final int w = bands[0].getRasterWidth();
        final int h = bands[0].getRasterHeight();
        final Rectangle rectangle = new Rectangle(0, 0, w, h);
        final Tile[] tiles = new Tile[bands.length];

        for (int i = 0; i < tiles.length; ++i) {
            tiles[i] = op.getSourceTile(bands[i], rectangle);
        }

        return new TilePixelAccessor(tiles);
    }

    private static PixelAccessor createPixelAccessor(Operator op, Product product, String[] bandNames) {
        final int w = product.getSceneRasterWidth();
        final int h = product.getSceneRasterHeight();
        final Rectangle rectangle = new Rectangle(0, 0, w, h);
        final Tile[] tiles = new Tile[bandNames.length];

        for (int i = 0; i < tiles.length; ++i) {
            tiles[i] = op.getSourceTile(product.getBand(bandNames[i]), rectangle);
        }

        return new TilePixelAccessor(tiles);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ExtractEndmembersOp.class);
        }
    }
}
