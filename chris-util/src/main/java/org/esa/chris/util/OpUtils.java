/*
 * Copyright (C) 2010-2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.chris.util;

import org.esa.chris.dataio.ChrisConstants;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.ProductUtils;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Operator utilities.
 *
 * @author Ralf Quast
 * @since CHRIS-BOX 1.0
 */
public class OpUtils {

    /**
     * Returns the index of the first band in a given list of bands which is accepted by a
     * given band filter.
     *
     * @param bands      the bands.
     * @param bandFilter the band filter.
     *
     * @return the index of the first band in {@code bands} which is accepted
     *         by {@code bandFilter}, or {@code -1} if no band is accepted.
     */
    public static int findBandIndex(final Band[] bands, BandFilter bandFilter) {
        for (int i = 0; i < bands.length; i++) {
            if (bandFilter.accept(bands[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the index of the band whose central wavelength is closest to
     * a wavelength of interest.
     *
     * @param bands      the bands.
     * @param wavelength the wavelength of interest.
     *
     * @return the index of the band index whose spectral wavelength is closest
     *         to the wavelength of interest.
     */
    public static int findBandIndex(final Band[] bands, final double wavelength) {
        return findBandIndex(bands, wavelength, Double.POSITIVE_INFINITY);
    }

    /**
     * Returns the index of the band whose central wavelength is closest to to
     * a wavelength of interest, but not farther than the given tolerance.
     *
     * @param bands      the bands.
     * @param wavelength the wavelength of interest.
     * @param tolerance  the tolerance.
     *
     * @return the index of the band index whose spectral wavelength is closest to
     *         {@code wavelength} or {@code -1} if the band with the closest wavelength
     *         is farther than {@code tolerance}.
     */
    public static int findBandIndex(final Band[] bands, final double wavelength, final double tolerance) {
        double minDelta = Math.abs(wavelength - bands[0].getSpectralWavelength());
        int minIndex = 0;

        for (int i = 1; i < bands.length; ++i) {
            final double delta = Math.abs(wavelength - bands[i].getSpectralWavelength());
            if (delta < minDelta) {
                minDelta = delta;
                minIndex = i;
            }
        }
        if (minDelta > tolerance) {
            minIndex = -1;
        }

        return minIndex;
    }

    /**
     * Returns the first band in a given list of bands which is accepted by a
     * given band filter.
     *
     * @param bands      the bands.
     * @param bandFilter the band filter.
     *
     * @return the band found or {@code null} if no band was found.
     */
    public static Band findBand(final Band[] bands, BandFilter bandFilter) {
        for (final Band band : bands) {
            if (bandFilter.accept(band)) {
                return band;
            }
        }

        return null;
    }

    /**
     * Returns an array of bands in a product of interest whose names start with
     * a given prefix.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     *
     * @return the bands found.
     */
    public static Band[] findBands(Product product, String prefix) {
        return findBands(product, prefix, new BandFilter() {
            @Override
            public boolean accept(Band band) {
                return true;
            }
        });
    }

    /**
     * Returns the first band in a product of interest whose name start with
     * a given prefix and is accepted by a given band filter.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     * @param filter  the band filter.
     *
     * @return the band found or {@code null} if no band was found.
     */
    public static Band findBand(Product product, String prefix, BandFilter filter) {
        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix) && filter.accept(band)) {
                return band;
            }
        }

        return null;
    }

    /**
     * Returns an array of bands in a product of interest whose names start with
     * a given prefix and are accepted by a given band filter.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     * @param filter  the band filter.
     *
     * @return the bands found.
     */
    public static Band[] findBands(Product product, String prefix, BandFilter filter) {
        final List<Band> bandList = new ArrayList<Band>(product.getBands().length);

        for (final Band band : product.getBands()) {
            if (band.getName().startsWith(prefix) && filter.accept(band)) {
                bandList.add(band);
            }
        }

        return bandList.toArray(new Band[bandList.size()]);
    }

    /**
     * Returns an array of bands in a product of interest whose names start with
     * a given prefix and are accepted by a given band filter.
     *
     * @param product the product of interest.
     * @param prefix  the prefix.
     * @param filter  the band filter.
     *
     * @return the bands found.
     */
    public static int[] findBandIndexes(Product product, String prefix, BandFilter filter) {
        final Band[] bands = product.getBands();
        final List<Integer> indexList = new ArrayList<Integer>(bands.length);

        for (int i = 0; i < bands.length; ++i) {
            if (bands[i].getName().startsWith(prefix) && filter.accept(bands[i])) {
                indexList.add(i);
            }
        }

        final int[] indexes = new int[indexList.size()];
        for (int i = 0; i < indexes.length; i++) {
            indexes[i] = indexList.get(i);
        }

        return indexes;
    }

    /**
     * Returns a CHRIS annotation as {@code double} for a product of interest.
     *
     * @param product      the product of interest.
     * @param name         the name of the CHRIS annotation.
     * @param defaultValue the default value returned when the annotation was
     *                     not found.
     *
     * @return the annotation as {@code double} or the default value if the
     *         annotation was not found.
     *
     * @throws OperatorException if the annotation was found but could not be parsed.
     */
    public static double getAnnotation(Product product, String name, double defaultValue) {
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            return defaultValue;
        }
        final String stringValue = element.getAttributeString(name, null);
        if (stringValue == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException ignore) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS annotation as {@code String} for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code String}.
     *
     * @throws OperatorException if the annotation could not be found.
     */
    public static String getAnnotationString(Product product, String name) throws OperatorException {
        final MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);

        if (element == null) {
            throw new OperatorException(MessageFormat.format("could not find CHRIS annotation ''{0}''", name));
        }
        final String stringValue = element.getAttributeString(name, null);

        if (stringValue == null) {
            throw new OperatorException(MessageFormat.format("could not find CHRIS annotation ''{0}''", name));
        }

        return stringValue;
    }

    /**
     * Sets a CHRIS annotation of a product of interest to a certain value.
     * If the metadata element for the requested CHRIS annotation does not
     * exists, a new metadata element is created.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     * @param value   the value.
     */
    public static void setAnnotationString(Product product, String name, String value) {
        MetadataElement element = product.getMetadataRoot().getElement(ChrisConstants.MPH_NAME);
        if (element == null) {
            element = new MetadataElement(ChrisConstants.MPH_NAME);
            product.getMetadataRoot().addElement(element);
        }
        element.setAttributeString(name, value);
    }

    /**
     * Returns a CHRIS annotation as {@code double} value for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code double} value.
     *
     * @throws OperatorException if the annotation could not be found or parsed.
     */
    public static double getAnnotationDouble(Product product, String name) throws OperatorException {
        final String stringValue = getAnnotationString(product, name);

        try {
            return Double.parseDouble(stringValue);
        } catch (NumberFormatException ignore) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS annotation as {@code int} value for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     *
     * @return the annotation as {@code int} value.
     *
     * @throws OperatorException if the annotation could not be found or parsed.
     */
    public static int getAnnotationInt(Product product, String name) throws OperatorException {
        final String stringValue = getAnnotationString(product, name);

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ignore) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns a CHRIS string annotation as {@code int} value for a product of interest.
     *
     * @param product the product of interest.
     * @param name    the name of the CHRIS annotation.
     * @param from    the first index of the annotation string to be considered.
     * @param to      the final index of the annotation string to be considered (exclusive).
     *
     * @return the annotation as {@code int} value.
     *
     * @throws OperatorException if the annotation could not be found or parsed.
     */
    public static int getAnnotationInt(Product product, String name, int from, int to) throws OperatorException {
        final String stringValue = getAnnotationString(product, name).substring(from, to);

        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ignore) {
            throw new OperatorException(MessageFormat.format("could not parse CHRIS annotation ''{0}''", name));
        }
    }

    /**
     * Returns the azimuthal difference angle between view and sun directions.
     *
     * @param vaa the view azimuth angle (degree).
     * @param saa the sun azimuth angle (degree).
     *
     * @return the azimuthal difference angle (degree).
     */
    public static double getAzimuthalDifferenceAngle(double vaa, double saa) {
        final double ada = Math.abs(vaa - saa);

        if (ada > 180.0) {
            return 360.0 - ada;
        }

        return ada;
    }

    /**
     * Returns the acquisition day (of year) for a product of interest.
     *
     * @param product the product of interst.
     *
     * @return the acquisition day (of year).
     */
    public static int getAcquisitionDay(Product product) {
        final ProductData.UTC utc = product.getStartTime();

        if (utc != null) {
            return utc.getAsCalendar().get(Calendar.DAY_OF_YEAR);
        } else {
            throw new OperatorException(MessageFormat.format("no date for product ''{0}''", product.getName()));
        }
    }

    /**
     * Returns the central wavelenghts for any spectral bands of interest.
     *
     * @param bands the bands of interest.
     *
     * @return the central wavelenghts (nm).
     */
    public static double[] getWavelenghts(Band[] bands) {
        final double[] wavelengths = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            wavelengths[i] = bands[i].getSpectralWavelength();
        }

        return wavelengths;
    }

    /**
     * Returns the bandwidths for any spectral bands of interest.
     *
     * @param bands the bands of interest.
     *
     * @return the bandwidths (nm).
     */
    public static double[] getBandwidths(Band[] bands) {
        final double[] bandwidths = new double[bands.length];

        for (int i = 0; i < bands.length; i++) {
            bandwidths[i] = bands[i].getSpectralBandwidth();
        }

        return bandwidths;
    }

    /**
     * Returns the correction factor for the solar irradiance due to the elliptical
     * orbit of the Sun.
     *
     * @param day the day (of year) of interest.
     *
     * @return the correction factor.
     */
    public static double getSolarIrradianceCorrectionFactor(int day) {
        final double d = 1.0 - 0.01673 * Math.cos(Math.toRadians(0.9856 * (day - 4)));

        return 1.0 / (d * d);
    }

    /**
     * Returns an {@link ImageInputStream} for a resource of interest.
     *
     * @param opClass the operator class used to get the resource.
     * @param name    the name of the resource.
     *
     * @return the image input stream.
     *
     * @throws OperatorException if the resource could not be found or the
     *                           image input stream could not be created.
     */
    public static ImageInputStream getResourceAsImageInputStream(Class<? extends Operator> opClass,
                                                                 String name) throws OperatorException {
        try {
            return getResourceAsStream(opClass, name);
        } catch (IOException e) {
            throw new OperatorException(MessageFormat.format(
                    "could not create image input stream for resource {0}", name), e);
        }
    }

    private static ImageInputStream getResourceAsStream(Class<?> aClass, String name) throws IOException {
        final InputStream is = aClass.getResourceAsStream(name);

        if (is == null) {
            throw new IOException(MessageFormat.format("resource {0} not found", name));
        }
        return new FileCacheImageInputStream(is, null);
    }

    public static double[][] readThuillierTable() throws OperatorException {
        final ImageInputStream iis;
        try {
            iis = getResourceAsStream(OpUtils.class, "thuillier.img");
        } catch (IOException e) {
            throw new OperatorException("could not read extraterrestrial solar irradiance table", e);
        }
        try {
            final int length = iis.readInt();
            final double[] abscissas = new double[length];
            final double[] ordinates = new double[length];

            iis.readFully(abscissas, 0, length);
            iis.readFully(ordinates, 0, length);

            return new double[][]{abscissas, ordinates};
        } catch (IOException e) {
            throw new OperatorException("could not read extraterrestrial solar irradiance table", e);
        } finally {
            try {
                iis.close();
            } catch (IOException ignore) {
            }
        }
    }

    public static Product createCopy(Product sourceProduct, String name, String type, BandFilter bandFilter) {
        final int w = sourceProduct.getSceneRasterWidth();
        final int h = sourceProduct.getSceneRasterHeight();
        final Product targetProduct = new Product(name, type, w, h);

        // 1. set start and end times
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        // 2. copy flag codings
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        // 3. copy all tie point grids
        for (final TiePointGrid sourceGrid : sourceProduct.getTiePointGrids()) {
            final String sourceName = sourceGrid.getName();
            final TiePointGrid targetGrid = ProductUtils.copyTiePointGrid(sourceName, sourceProduct, targetProduct);
            ProductUtils.copyRasterDataNodeProperties(sourceGrid, targetGrid);
            targetGrid.setSourceImage(sourceGrid.getSourceImage());
        }
        // 4. copy all bands matching the filter
        for (final Band sourceBand : sourceProduct.getBands()) {
            if (bandFilter.accept(sourceBand)) {
                final String sourceName = sourceBand.getName();
                final Band targetBand = ProductUtils.copyBand(sourceName, sourceProduct, targetProduct,true);
                final FlagCoding flagCoding = sourceBand.getFlagCoding();
                if (flagCoding != null) {
                    targetBand.setSampleCoding(targetProduct.getFlagCodingGroup().get(flagCoding.getName()));
                }
            }
        }
        // 5. copy masks
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        // 6. copy meta data
        ProductUtils.copyMetadata(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        // 7. copy pins
        for (int i = 0; i < sourceProduct.getPinGroup().getNodeCount(); i++) {
            final Placemark pin = sourceProduct.getPinGroup().get(i);
            targetProduct.getPinGroup().add(Placemark.createPointPlacemark(pin.getDescriptor(),
                                           pin.getName(),
                                           pin.getLabel(),
                                           pin.getDescription(),
                                           pin.getPixelPos(),
                                           pin.getGeoPos(),
                                           sourceProduct.getSceneGeoCoding()));
        }
        // 8. copy GCPs
        for (int i = 0; i < sourceProduct.getGcpGroup().getNodeCount(); i++) {
            final Placemark gcp = sourceProduct.getGcpGroup().get(i);
            targetProduct.getGcpGroup().add(Placemark.createPointPlacemark(gcp.getDescriptor(),
                                                                           gcp.getName(),
                                                                           gcp.getLabel(),
                                                                           gcp.getDescription(),
                                                                           gcp.getPixelPos(),
                                                                           gcp.getGeoPos(),
                                                                           sourceProduct.getSceneGeoCoding()));
        }
        // 9. copy pins
        for (int i = 0; i < sourceProduct.getPinGroup().getNodeCount(); i++) {
            final Placemark pin = sourceProduct.getPinGroup().get(i);
            targetProduct.getGcpGroup().add(Placemark.createPointPlacemark(pin.getDescriptor(),
                                                                           pin.getName(),
                                                                           pin.getLabel(),
                                                                           pin.getDescription(),
                                                                           pin.getPixelPos(),
                                                                           pin.getGeoPos(),
                                                                           sourceProduct.getSceneGeoCoding()));
        }
        // 10. set preferred tile size
        targetProduct.setPreferredTileSize(sourceProduct.getPreferredTileSize());

        targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());
        return targetProduct;
    }
}
