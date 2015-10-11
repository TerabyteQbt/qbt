package qbt.recursive.rpd;

import qbt.NormalDependencyType;
import qbt.recursive.rd.RecursiveDataMapper;

public abstract class RecursivePackageDataMapper<V, R extends RecursivePackageData<V, R>, OUTPUT> extends RecursiveDataMapper<String, NormalDependencyType, V, R, OUTPUT>{
}
