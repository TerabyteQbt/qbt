package qbt;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import misc1.commons.ph.ProcessHelper;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import qbt.mains.BuildPorcelain;
import qbt.mains.RunOverridesPorcelain;
import qbt.utils.TarballUtils;

public class IntegrationTests {
    @Rule
    public final TemporaryFolder temporaryFolderRule = new TemporaryFolder();

    private Path unpackWorkspace(String workspaceName) throws IOException {
        final URL tarballUrl = IntegrationTests.class.getClassLoader().getResource("META-INF/qbt/test-repos/IntegrationTests-" + workspaceName + ".tar.gz");
        Path tarballFile = temporaryFolderRule.newFile().toPath();
        try(InputStream is = Resources.asByteSource(tarballUrl).openStream(); OutputStream os = QbtUtils.openWrite(tarballFile)) {
            ByteStreams.copy(is, os);
        }
        Path workspace = temporaryFolderRule.newFolder().toPath();
        TarballUtils.explodeTarball(workspace, tarballFile);
        QbtUtils.delete(tarballFile);
        return workspace;
    }

    @Test
    public void test1() throws Exception {
        runBuild(unpackWorkspace("test1"), "--all");
    }

    @Test
    public void testRebuild() throws Exception {
        final Path workspace = unpackWorkspace("testRebuild");

        class Helper {
            private int outputId = 1;
            private Path outputDir;

            public void build() throws Exception {
                outputDir = workspace.resolve("output" + outputId++);
                runBuild(workspace, "--package", "p2", "--output", "all,directory," + outputDir.resolve("%p"));
            }

            public void checkContent(String p1, String p2) throws IOException {
                Assert.assertEquals(p1, readLine(outputDir.resolve("p1/content")));
                Assert.assertEquals(p2, readLine(outputDir.resolve("p2/content")));
                Assert.assertEquals(p1, readLine(outputDir.resolve("p2/p1/content")));
            }

            public String checkRandom1(String exp) throws IOException {
                String obs = readLine(outputDir.resolve("p1/random"));
                if(exp != null) {
                    Assert.assertEquals(exp, obs);
                }
                String obs2 = readLine(outputDir.resolve("p2/p1/random"));
                Assert.assertEquals(obs, obs2);
                return obs;
            }

            public String checkRandom2(String exp) throws IOException {
                String obs = readLine(outputDir.resolve("p2/random"));
                if(exp != null) {
                    Assert.assertEquals(exp, obs);
                }
                return obs;
            }
        }
        Helper h = new Helper();

        h.build();
        h.checkContent("1", "1");
        String p1c1Random = h.checkRandom1(null);
        String p2c11Random = h.checkRandom2(null);

        h.build();
        h.checkContent("1", "1");
        h.checkRandom1(p1c1Random);
        h.checkRandom2(p2c11Random);

        writeLine(workspace.resolve("local/HEAD/r1/p2/content"), "2");
        h.build();
        h.checkContent("1", "2");
        h.checkRandom1(p1c1Random);
        String p2c12Random = h.checkRandom2(null);
        Assert.assertNotEquals(p2c11Random, p2c12Random);

        ProcessHelper.of(workspace.resolve("local/HEAD/r1"), "git", "add", "-A").run().requireSuccess();
        for(int i = 0; i < 2; ++i) {
            h.build();
            h.checkContent("1", "2");
            h.checkRandom1(p1c1Random);
            h.checkRandom2(p2c12Random);
        }

        ProcessHelper.of(workspace.resolve("local/HEAD/r1"), "git", "commit", "-a", "-m.").run().requireSuccess();
        for(int i = 0; i < 2; ++i) {
            h.build();
            h.checkContent("1", "2");
            h.checkRandom1(p1c1Random);
            h.checkRandom2(p2c12Random);
        }

        writeLine(workspace.resolve("local/HEAD/r1/p2/content"), "1");
        for(int i = 0; i < 2; ++i) {
            h.build();
            h.checkContent("1", "1");
            h.checkRandom1(p1c1Random);
            h.checkRandom2(p2c11Random);
        }

        writeLine(workspace.resolve("local/HEAD/r1/p1/content"), "2");
        h.build();
        h.checkContent("2", "1");
        String p1c2Random = h.checkRandom1(null);
        Assert.assertNotEquals(p1c1Random, p1c2Random);
        String p2c21Random = h.checkRandom2(null);
        Assert.assertNotEquals(p2c11Random, p2c21Random);
        Assert.assertNotEquals(p2c12Random, p2c21Random);

        h.build();
        h.checkContent("2", "1");
        h.checkRandom1(p1c2Random);
        h.checkRandom2(p2c21Random);

        writeLine(workspace.resolve("local/HEAD/r1/p1/content"), "1");
        for(int i = 0; i < 2; ++i) {
            h.build();
            h.checkContent("1", "1");
            h.checkRandom1(p1c1Random);
            h.checkRandom2(p2c11Random);
        }
    }

    @Test
    public void testRunOverrides() throws Exception {
        Path workspace = unpackWorkspace("testRunOverrides");
        String[] packages = new String[] {"p1", "p2", "p3"};
        for(String pa : packages) {
            for(String pb : packages) {
                if(pa.equals(pb)) {
                    Assert.assertEquals(0, runInstance(workspace, new RunOverridesPorcelain(), "--package", pa, "--", "sh", "-c", "if [ '!' -f " + pb + ".dir]; then exit 1; fi"));
                }
                else {
                    Assert.assertEquals(0, runInstance(workspace, new RunOverridesPorcelain(), "--package", pa, "--", "sh", "-c", "if [ -f " + pb + ".dir]; then exit 1; fi"));
                }
            }
        }
        Assert.assertEquals(0, runInstance(workspace, new RunOverridesPorcelain(), "--all", "--", "sh", "-c", "if [ '!' -f $REPO_NAME.dir]; then exit 1; fi"));
    }

    private String readLine(Path file) throws IOException {
        return Iterables.getOnlyElement(QbtUtils.readLines(file));
    }

    private static void writeLine(Path file, String line) {
        QbtUtils.writeLines(file, ImmutableList.of(line));
        // This is pathetic but e.g. `git ls-files -m` can fail to register a
        // changed file immediately, presumably due to mtime checking
        // shennanigans.  We force the index to drop anything it thinks it
        // knows.
        if(ProcessHelper.of(file.getParent(), "git", "rev-parse", "--git-dir").run().exitCode == 0) {
            ProcessHelper.of(file.getParent(), "git", "update-index", "--no-assume-unchanged", file.getFileName().toString()).run().requireSuccess();
        }
    }

    private void runBuild(Path workspace, String... args) throws Exception {
        Assert.assertEquals(0, runInstance(workspace, new BuildPorcelain(), args));
    }

    private int runInstance(Path workspace, QbtCommand<?> instance, String...args) throws Exception {
        ImmutableList.Builder<String> argsBuilder = ImmutableList.builder();
        argsBuilder.add("--config");
        argsBuilder.add(workspace.resolve("qbt-config").toAbsolutePath().toString());
        argsBuilder.add("--manifest");
        argsBuilder.add(workspace.resolve("qbt-manifest").toAbsolutePath().toString());
        for(String arg : args) {
            argsBuilder.add(arg);
        }
        return QbtMain.runInstance(instance, argsBuilder.build(), false);
    }
}
