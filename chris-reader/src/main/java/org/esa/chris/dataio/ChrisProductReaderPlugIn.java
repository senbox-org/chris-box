/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.chris.dataio;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.util.Locale;

public class ChrisProductReaderPlugIn implements ProductReaderPlugIn {

    static{
        registerRGBProfiles();
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            return DecodeQualification.UNABLE;
        }

        if (file.isFile() && file.getPath().toLowerCase().endsWith(ChrisConstants.DEFAULT_FILE_EXTENSION)) {
            NetcdfFile ncFile = null;
            try {
                ncFile = NetcdfFileOpener.open(file.getAbsolutePath());
                if (ncFile == null) {
                    return DecodeQualification.UNABLE;
                }
                
                if (isSensorTypeAttributeCorrect(ncFile)) {
                    return DecodeQualification.INTENDED;
                }
            } catch (Throwable ignore) {
                // nothing to do, return value is already false
            } finally {
                if (ncFile != null) {
                    try {
                        ncFile.close();
                    } catch (Exception ignore) {
                        // nothing to do, return value is already false
                    }
                }
            }
        }

        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new ChrisProductReader(this);
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new SnapFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{ChrisConstants.DEFAULT_FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(Locale locale) {
        return ChrisConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{ChrisConstants.FORMAT_NAME};
    }

    private static boolean isSensorTypeAttributeCorrect(NetcdfFile ncFile) throws Exception {
        Attribute attribute = ncFile.findGlobalAttributeIgnoreCase(ChrisConstants.ATTR_NAME_SENSOR_TYPE);
        return (attribute != null && 
                attribute.getDataType() == DataType.STRING &&
                attribute.getStringValue().equalsIgnoreCase("CHRIS"));
     }

    private static void registerRGBProfiles() {
        RGBImageProfileManager profileManager = RGBImageProfileManager.getInstance();
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 0",
                                                      new String[]{
                                                              "radiance_3",
                                                              "radiance_2",
                                                              "radiance_1"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 1",
                                                      new String[]{
                                                              "radiance_23",
                                                              "radiance_13",
                                                              "radiance_2"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 2",
                                                      new String[]{
                                                              "radiance_10",
                                                              "radiance_6",
                                                              "radiance_2"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 3",
                                                      new String[]{
                                                              "radiance_7",
                                                              "radiance_4",
                                                              "radiance_1"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 3A",
                                                      new String[]{
                                                              "radiance_8",
                                                              "radiance_5",
                                                              "radiance_2"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 4",
                                                      new String[]{
                                                              "radiance_4",
                                                              "radiance_2",
                                                              "radiance_1"
                                                      }));
        profileManager.addProfile(new RGBImageProfile("CHRIS/Proba - Mode 5",
                                                      new String[]{
                                                              "radiance_7",
                                                              "radiance_4",
                                                              "radiance_1"
                                                      }));
    }

}
