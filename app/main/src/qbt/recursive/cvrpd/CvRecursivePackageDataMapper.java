package qbt.recursive.cvrpd;

import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.rpd.RecursivePackageDataMapper;

public abstract class CvRecursivePackageDataMapper<V, OUTPUT> extends RecursivePackageDataMapper<Pair<CumulativeVersion, V>, CvRecursivePackageData<V>, OUTPUT> {
}
