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
package org.esa.chris.geocorr.operators;

import org.esa.chris.geocorr.operators.IctDataRecord.IctDataReader;
import org.esa.chris.geocorr.operators.TelemetryFinder.Telemetry;
import org.esa.chris.util.OpUtils;
import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.PointingFactoryRegistry;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Operator performing the geometric correction.
 *
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
@OperatorMetadata(alias = "chris.PerformGeometricCorrection",
        version = "1.0",
        authors = "Ralf Quast, Marco Zühlke",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Performs the geometric correction for a CHRIS/Proba RCI.")
public class PerformGeometricCorrectionOp extends Operator {

    /**
     * The alias of the {@code telemetryRepository} parameter.
     */
    public static final String ALIAS_TELEMETRY_REPOSITORY = "telemetryRepository";
    /**
     * The delay of 0.999s between the GPS time tag and the actual time of the reported position.
     */
    public static final double DELAY = 0.999;

    @Parameter(alias = ALIAS_TELEMETRY_REPOSITORY, label = "Telemetry repository", defaultValue = ".",
            description = "The directory searched for CHRIS telemetry data", notNull = true, notEmpty = true)
    private File telemetryRepository;

    @Parameter(label = "Use target altitude", defaultValue = "true",
            description = "If true, the pixel lines-of-sight are intersected with a modified WGS-84 ellipsoid," +
                          "which increased by the nominal target altitude")
    private boolean useTargetAltitude;

    @Parameter(label = "Include pitch and roll angles (for diagnostics only)", defaultValue = "false",
            description = "If true, the target product will include instrument pitch and roll angles per pixel")
    private boolean includePitchAndRoll;

    @SourceProduct(type = "CHRIS_M[012345][0A]?(_NR)?(_TOA_REFL)?(_AC)?")
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        try {
            // 1. get the telemetry
            final Telemetry telemetry = TelemetryFinder.findTelemetry(sourceProduct, telemetryRepository);

            // 2. get GPS time delay from image center time
            final Date ict = sourceProduct.getStartTime().getAsDate();
            final double mjd = TimeConverter.dateToMJD(ict);
            final double deltaGPS = TimeConverter.getInstance().deltaGPS(mjd);

            // 3. read image center times from telemetry
            final IctDataRecord ictData = readIctData(telemetry.getIctFile(), deltaGPS);

            // 4. read trajectory from telemetry
            final List<GpsDataRecord> gpsData = readGpsData(telemetry.getGpsFile(), deltaGPS, DELAY);

            // 5. create acquisition info
            final AcquisitionInfo info = AcquisitionInfo.create(sourceProduct);

            // 6. create GCPs
            final GCP[] gcps = GCP.createArray(sourceProduct.getGcpGroup(), info.getTargetAlt());

            // 7. calculate geometry
            final GeometryCalculator calculator = new GeometryCalculator(ictData, gpsData, info, gcps);
            calculator.calculate(useTargetAltitude);

            // 8. create and set the target product
            setTargetProduct(createTargetProduct(calculator, info.isBackscanning()));
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    private Product createTargetProduct(GeometryCalculator calculator, boolean backscanning) {
        final String productType = sourceProduct.getProductType() + "_GC";
        final Product targetProduct = OpUtils.createCopy(sourceProduct, "GC", productType, band -> true);

        final int w = targetProduct.getSceneRasterWidth();
        final int h = targetProduct.getSceneRasterHeight();

        int size = w * h;
        final float[] lons = new float[size];
        final float[] lats = new float[size];
        final float[] vaas = new float[size];
        final float[] vzas = new float[size];

        for (int row = 0; row < h; row++) {
            final int y = backscanning ? h - 1 - row : row;
            for (int x = 0; x < w; x++) {
                lons[row * w + x] = (float) calculator.getLon(x, y);
                lats[row * w + x] = (float) calculator.getLat(x, y);
                vaas[row * w + x] = (float) calculator.getVaa(x, y);
                vzas[row * w + x] = (float) calculator.getVza(x, y);
            }
        }

        final String lonVarName = "lon";
        final String latVarName = "lat";
        addTiePointGrid(targetProduct, lonVarName, w, h, lons, "Longitude (deg)", "deg");
        addTiePointGrid(targetProduct, latVarName, w, h, lats, "Latitude (deg)", "deg");
        addTiePointGrid(targetProduct, "vaa", w, h, vaas, "View azimuth angle (deg)", "deg");
        addTiePointGrid(targetProduct, "vza", w, h, vzas, "View zenith angle (deg)", "deg");

        if (includePitchAndRoll) {
            final float[] ipas = new float[size];
            final float[] iras = new float[size];
            for (int row = 0; row < h; row++) {
                final int y = backscanning ? h - 1 - row : row;
                for (int x = 0; x < w; x++) {
                    ipas[row * w + x] = (float) calculator.getPitch(x, y);
                    iras[row * w + x] = (float) calculator.getRoll(x, y);
                }
            }
            addTiePointGrid(targetProduct, "ipa", w, h, ipas, "Instrument pitch angle (rad)", "rad");
            addTiePointGrid(targetProduct, "ira", w, h, iras, "Instrument roll angle (rad)", "rad");
        }
        double[] lonData = new double[size];
        double[] latData = new double[size];
        for (int i = 0; i < lons.length; i++) {
            lonData[i] = lons[i];
            latData[i] = lats[i];
        }
        double resolutionInKm = RasterUtils.computeResolutionInKm(lonData, latData, w, h);
        final GeoRaster geoRaster = new GeoRaster(lonData, latData, lonVarName, latVarName,
                                                  w, h, resolutionInKm);
        final ForwardCoding forward = ComponentFactory.getForward(TiePointBilinearForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(TiePointInverse.KEY);
        targetProduct.setSceneGeoCoding(new ComponentGeoCoding(geoRaster, forward, inverse, GeoChecks.ANTIMERIDIAN));

        targetProduct.setPointingFactory(PointingFactoryRegistry.getInstance().getPointingFactory(productType));

        return targetProduct;
    }

    private static TiePointGrid addTiePointGrid(Product targetProduct, String name, int w, int h, float[] tiePoints,
                                                String description, String unit) {
        final TiePointGrid lonGrid = new TiePointGrid(name, w, h, 0.5f, 0.5f, 1.0f, 1.0f, tiePoints);
        lonGrid.setDescription(description);
        lonGrid.setUnit(unit);
        targetProduct.addTiePointGrid(lonGrid);

        return lonGrid;
    }

    static List<GpsDataRecord> readGpsData(File gpsFile, double deltaGPS, double delay) throws IOException {
        try (InputStream is = new FileInputStream(gpsFile)) {
            return GpsDataRecord.create(new GpsDataRecord.GpsDataReader(is).getReadRecords(), deltaGPS, delay);
        }
    }

    static IctDataRecord readIctData(File ictFile, double deltaGPS) throws IOException {
        try (InputStream is = new FileInputStream(ictFile)) {
            return IctDataRecord.create(new IctDataReader(is).getLastIctValues(), deltaGPS);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PerformGeometricCorrectionOp.class);
        }
    }
}
