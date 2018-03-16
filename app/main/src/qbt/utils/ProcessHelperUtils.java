package qbt.utils;

import misc1.commons.ph.ProcessHelper;

public final class ProcessHelperUtils {
    private ProcessHelperUtils() {
        // nope
    }

    public static ProcessHelper stripGitEnv(ProcessHelper p) {
        // Fuckers, these break all sorts of things and are also insane.  `git`
        // is so bad at handling environment variables if `qbt` is run
        // underneath git at any point (e.g.  mergeDriver) all hell breaks
        // loose.
        p = p.transform(ProcessHelper.ENV, (env) -> {
            for(String key : env.keys()) {
                if(key.startsWith("GIT_")) {
                    env = env.simpleRemove(key);
                }
            }
            return env;
        });
        return p;
    }

    public static ProcessHelper.Callback<?> simplePrefixCallback(final String prefix) {
        return new ProcessHelper.Callback<Void>() {
            @Override
            public void line(boolean isError, String line) {
                (isError ? System.err : System.out).println("[" + prefix + "] " + line);
            }

            @Override
            public Void complete(int exitCode) {
                if(exitCode != 0) {
                    throw new RuntimeException("[" + prefix + "] Non-zero exit: " + exitCode);
                }
                return null;
            }
        };
    }
}
