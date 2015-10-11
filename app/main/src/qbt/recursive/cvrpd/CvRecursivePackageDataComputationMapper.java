package qbt.recursive.cvrpd;

import org.apache.commons.lang3.tuple.Pair;
import qbt.recursive.cv.CumulativeVersion;
import qbt.recursive.rpd.RecursivePackageDataComputationMapper;

public abstract class CvRecursivePackageDataComputationMapper<V, R extends CvRecursivePackageData<V>, OUTPUT> extends RecursivePackageDataComputationMapper<Pair<CumulativeVersion, V>, CvRecursivePackageData<V>, OUTPUT> {
}
