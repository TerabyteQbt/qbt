package qbt.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import misc1.commons.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.QbtHashUtils;

public final class ProcessHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessHelper.class);

    private ProcessBuilder pb;
    private final Path dir;
    private final String desc;

    private static final Path DEV_NULL = Paths.get("/dev/null");

    public ProcessHelper(Path dir, String... args) {
        pb = new ProcessBuilder(args);
        pb = pb.directory(dir.toFile());

        // Fuckers, these break all sorts of things and are also insane.  `git`
        // is so bad at handling environment variables if `qbt` is run
        // underneath git at any point (e.g.  mergeDriver) all hell breaks
        // loose.
        pb.environment().remove("GIT_DIR");
        pb.environment().remove("GIT_WORK_DIR");

        this.dir = dir;
        this.desc = Joiner.on(' ').join(args);
    }

    public ProcessHelper inheritInput() {
        pb = pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        return this;
    }

    public ProcessHelper ignoreOutput() {
        pb = pb.redirectOutput(DEV_NULL.toFile());
        return this;
    }

    public ProcessHelper inheritOutput() {
        pb = pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        return this;
    }

    public ProcessHelper ignoreError() {
        pb = pb.redirectError(DEV_NULL.toFile());
        return this;
    }

    public ProcessHelper inheritError() {
        pb = pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        return this;
    }

    public ProcessHelper combineError() {
        pb = pb.redirectErrorStream(true);
        return this;
    }

    public ProcessHelper fileInput(Path path) {
        pb = pb.redirectInput(path.toFile());
        return this;
    }

    public ProcessHelper putEnv(String key, String value) {
        pb.environment().put(key, value);
        return this;
    }

    public ProcessHelper stripEnv(Predicate<Pair<String, String>> predicate) {
        Iterator<Map.Entry<String, String>> i = pb.environment().entrySet().iterator();
        while(i.hasNext()) {
            Map.Entry<String, String> e = i.next();
            if(predicate.apply(Pair.of(e.getKey(), e.getValue()))) {
                i.remove();
            }
        }
        return this;
    }

    private interface Callback<V> {
        void push(String line);
        V complete(int exit);
    }

    private abstract class DemandSuccessCallback<V> implements Callback<V> {
        @Override
        public V complete(int exit) {
            if(exit != 0) {
                throw new RuntimeException(desc + " failed: " + exit);
            }
            return complete();
        }

        protected abstract V complete();
    }

    private class VoidCallback extends DemandSuccessCallback<Void> {
        @Override
        public void push(String line) {
        }

        @Override
        public Void complete() {
            return null;
        }
    }

    private class WasSuccessCallback implements Callback<Boolean> {
        @Override
        public void push(String line) {
        }

        @Override
        public Boolean complete(int exit) {
            return exit == 0;
        }
    }

    private <V> V completeCommon(Callback<V> cb) {
        try {
            if(LOGGER.isDebugEnabled()) {
                LOGGER.debug("Running " + desc + " in " + dir + "...");
            }
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), Charsets.UTF_8));
            try {
                while(true) {
                    String line = br.readLine();
                    if(line == null) {
                        return cb.complete(p.waitFor());
                    }
                    cb.push(line);
                }
            }
            finally {
                br.close();
            }
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    public boolean completeWasSuccess() {
        return completeCommon(new WasSuccessCallback());
    }

    public String completeLine() {
        List<String> lines = completeLines();
        if(lines.size() != 1) {
            throw new RuntimeException(desc + " did not print exactly one line?");
        }
        return lines.get(0);
    }

    public HashCode completeSha1() {
        return QbtHashUtils.parse(completeLine().substring(0, 40));
    }

    public ImmutableList<String> completeLines() {
        return completeCommon(new DemandSuccessCallback<ImmutableList<String>>() {
            private final ImmutableList.Builder<String> b = ImmutableList.builder();

            @Override
            public void push(String line) {
                b.add(line);
            }

            @Override
            public ImmutableList<String> complete() {
                return b.build();
            }
        });
    }

    public void completeVoid() {
        completeCommon(new VoidCallback());
    }

    public void completeLinesCallback(final Function<String, Void> lineHandler) {
        completeCommon(new DemandSuccessCallback<Void>() {
            @Override
            public void push(String line) {
                lineHandler.apply(line);
            }

            @Override
            public Void complete() {
                return null;
            }
        });
    }

    public boolean completeWasEmpty() {
        return completeCommon(new DemandSuccessCallback<Boolean>() {
            private boolean ret = true;

            @Override
            public void push(String line) {
                ret = false;
            }

            @Override
            public Boolean complete() {
                return ret;
            }
        });
    }

    public int completeExitCode() {
        return completeLinesCallbackExitCode(null);
    }

    public int completeLinesCallbackExitCode(final Function<String, Void> lineHandler) {
        return completeCommon(new Callback<Integer>() {
            @Override
            public void push(String line) {
                if(lineHandler != null) {
                    lineHandler.apply(line);
                }
            }

            @Override
            public Integer complete(int exit) {
                return exit;
            }
        });
    }

    public Pair<ImmutableList<String>, Integer> completeLinesAndExitCode() {
        return completeCommon(new Callback<Pair<ImmutableList<String>, Integer>>() {
            private final ImmutableList.Builder<String> b = ImmutableList.builder();

            @Override
            public void push(String line) {
                b.add(line);
            }

            @Override
            public Pair<ImmutableList<String>, Integer> complete(int exit) {
                return Pair.of(b.build(), exit);
            }
        });
    }

    public String getDescription() {
        return desc;
    }
}
