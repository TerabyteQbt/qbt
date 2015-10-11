package qbt.recursive.rpd;

import qbt.NormalDependencyType;
import qbt.recursive.rd.RecursiveDataComputationMapper;

public abstract class RecursivePackageDataComputationMapper<V, R extends RecursivePackageData<V, R>, OUTPUT> extends RecursiveDataComputationMapper<String, NormalDependencyType, V, R, OUTPUT> {
}
