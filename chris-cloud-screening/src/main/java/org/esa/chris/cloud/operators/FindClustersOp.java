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
import org.esa.chris.cloud.operators.lut.Clusterer;
import org.esa.snap.cluster.EMCluster;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;

import java.awt.Rectangle;
import java.util.Comparator;

/**
 * Clustering operator.
 *
 * @author Ralf Quast
 */
@OperatorMetadata(alias = "chris.FindClusters",
                  version = "1.1",
                  authors = "Ralf Quast",
                  copyright = "(c) 2008-2020 by Brockmann Consult",
                  description = "Performs an expectation-maximization (EM) cluster analysis.",
                  internal = true)
public class FindClustersOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProperty
    private EMCluster[] clusters;

    @Parameter(label = "Source bands", rasterDataNodeType = Band.class)
    private String[] sourceBandNames;
    @Parameter(label = "Number of clusters", defaultValue = "14", interval = "[2,99]")
    private int clusterCount;
    @Parameter(label = "Number of iterations", defaultValue = "30", interval = "[1,999]")
    private int iterationCount;
    @Parameter(label = "Random seed",
               defaultValue = "31415",
               description = "The seed used for initializing the EM clustering algorithm.")
    private int seed;

    public FindClustersOp() {
    }

    private FindClustersOp(Product sourceProduct, int clusterCount, int iterationCount, int seed,
                           String[] sourceBandNames) {
        this.sourceProduct = sourceProduct;
        this.clusterCount = clusterCount;
        this.iterationCount = iterationCount;
        this.seed = seed;
        this.sourceBandNames = sourceBandNames;
    }

    @Override
    public void initialize() throws OperatorException {
        final Comparator<EMCluster> comparator = new Clusterer.PriorProbabilityClusterComparator();
        clusters = new EMCluster[clusterCount];
        findClusters(sourceProduct, sourceBandNames, clusters, iterationCount, seed, comparator,
                     ProgressMonitor.NULL);
        setTargetProduct(new Product("NULL", "NULL", 0, 0));
    }

    public static void findClusters(Product sourceProduct,
                                    String[] sourceBandNames,
                                    EMCluster[] clusters,
                                    int iterationCount,
                                    int seed,
                                    Comparator<EMCluster> clusterComparator,
                                    ProgressMonitor pm) {
        final FindClustersOp op = new FindClustersOp(sourceProduct, clusters.length, iterationCount, seed,
                                                     sourceBandNames);

        final Tile[] tiles = new Tile[sourceBandNames.length];
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();

        try {
            pm.beginTask("Performing cluster analysis...", iterationCount);

            final Rectangle sourceRectangle = new Rectangle(0, 0, w, h);
            for (int i = 0; i < sourceBandNames.length; i++) {
                tiles[i] = op.getSourceTile(sourceProduct.getBand(sourceBandNames[i]),
                                            sourceRectangle);
            }

            final Clusterer clusterer = new Clusterer(new TilePixelAccessor(tiles), clusters.length, seed);
            for (int i = 0; i < iterationCount; ++i) {
                op.checkForCancellation();
                clusterer.iterate();
                pm.worked(1);
            }
            clusterer.getClusters(clusterComparator, clusters);
        } catch (OperatorException e) {
            throw e;
        } catch (Throwable t) {
            throw new OperatorException(t);
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FindClustersOp.class);
        }
    }

}
