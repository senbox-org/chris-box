package org.esa.chris.geocorr.operators;

import com.bc.ceres.core.ProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * @author Marco Peters
 * @since Chris-Box 3.0
 */
public interface TimeTableDecoder {

    ConcurrentNavigableMap<Double, Double> decode(InputStream streamToDecode, ProgressMonitor pm) throws IOException;

}
