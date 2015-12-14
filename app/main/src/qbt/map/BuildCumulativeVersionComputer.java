package qbt.map;

import qbt.QbtManifest;
import qbt.config.QbtConfig;
import qbt.recursive.cv.CumulativeVersionNodeData;

// If you're just using the results to build then only CV matters.
public class BuildCumulativeVersionComputer extends CumulativeVersionComputer<CumulativeVersionNodeData> {
    public BuildCumulativeVersionComputer(QbtConfig config, QbtManifest manifest) {
        super(config, manifest);
    }

    @Override
    protected CumulativeVersionNodeData canonicalizationKey(CumulativeVersionComputer.Result result) {
        return result.cumulativeVersionNodeData;
    }
}