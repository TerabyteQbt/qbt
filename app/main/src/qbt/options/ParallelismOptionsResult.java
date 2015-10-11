package qbt.options;

import misc1.commons.concurrent.WorkPool;

public interface ParallelismOptionsResult {
    WorkPool createWorkPool();
}
