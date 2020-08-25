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
package org.esa.chris.operators;

import org.esa.snap.core.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;


/**
 * Handle image center time reading and calculation.
 *
 * @author Marco Zuehlke
 * @since CHRIS-Box 1.5
 */
class IctDataRecord {

    private static final double DAY_SEC = 86400.0;
    // Julian Date (JD) of 1999-12-26 00:00
    private static final double JD0 = TimeConverter.julianDate(1999, 11, 26);

    final double ict1;
    final double ict2;
    final double ict3;
    final double ict4;
    final double ict5;

    IctDataRecord(double ict1, double ict2, double ict3, double ict4, double ict5) {
        this.ict1 = ict1;
        this.ict2 = ict2;
        this.ict3 = ict3;
        this.ict4 = ict4;
        this.ict5 = ict5;
    }

    static IctDataRecord create(double[] ictValues, double dTgps) throws IOException {
        double ict1 = getUT1((ictValues[0] - dTgps) / DAY_SEC + JD0);
        double ict2 = getUT1((ictValues[1] - dTgps) / DAY_SEC + JD0);
        double ict3 = getUT1((ictValues[2] - dTgps) / DAY_SEC + JD0);
        double ict4 = getUT1((ictValues[3] - dTgps) / DAY_SEC + JD0);
        double ict5 = getUT1((ictValues[4] - dTgps) / DAY_SEC + JD0);
        return new IctDataRecord(ict1, ict2, ict3, ict4, ict5);
    }

    static class IctDataReader {

        private final String[] lastIctRecord;

        IctDataReader(InputStream is) {
            Reader reader = new InputStreamReader(is);
            char[] separators = new char[]{'\t'};
            CsvReader csvReader = new CsvReader(reader, separators, true, "TIME");
            String[] record = null;
            try {
                record = readRecords(csvReader);
            } catch (IOException e) {
                try {
                    csvReader.close();
                } catch (IOException e1) {
                    // ignore
                }
            }
            lastIctRecord = record;
        }

        private String[] readRecords(CsvReader csvReader) throws IOException {
            List<String[]> stringRecords = csvReader.readStringRecords();
            int ndxICT = -1;
            for (int i = 0; i < stringRecords.size(); i++) {
                String[] record = stringRecords.get(i);
                if (record[ICT.ICT1_PKT.index].startsWith("+G:29494")) {
                    ndxICT = i;
                }
            }
            if (ndxICT != -1) {
                return stringRecords.get(ndxICT);
            } else {
                return null;
            }
        }

        String[] getLastIctRecord() {
            return lastIctRecord;
        }

        double[] getLastIctValues() {
            double[] ict = new double[5];
            ict[0] = Double.parseDouble(lastIctRecord[ICT.ICT1.index]);
            ict[1] = Double.parseDouble(lastIctRecord[ICT.ICT2.index]);
            ict[2] = Double.parseDouble(lastIctRecord[ICT.ICT3.index]);
            ict[3] = Double.parseDouble(lastIctRecord[ICT.ICT4.index]);
            ict[4] = Double.parseDouble(lastIctRecord[ICT.ICT5.index]);
            return ict;
        }
    }

    private enum ICT {

        TIME(0),
        PKT(1),
        FLYBY_PKT(2),
        FLYBY(3),
        ICT1_PKT(4),
        ICT1(5),
        ICT2_PKT(6),
        ICT2(7),
        ICT3_PKT(8),
        ICT3(9),
        ICT4_PKT(10),
        ICT4(11),
        ICT5_PKT(12),
        ICT5(13);

        final int index;

        ICT(int index) {
            this.index = index;
        }
    }

    private static double getUT1(double jd) throws IOException {
        return jd + TimeConverter.getInstance().deltaUT1(TimeConverter.jdToMJD(jd)) / TimeConverter.SECONDS_PER_DAY;
    }

}
