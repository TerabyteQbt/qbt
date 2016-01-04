package qbt.artifactcacher;

import com.google.common.base.Function;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import misc1.commons.ExceptionUtils;
import misc1.commons.resources.FreeScope;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.QbtHashUtils;
import qbt.QbtTempDir;
import qbt.QbtUtils;
import qbt.recursive.cv.CumulativeVersionDigest;

public class UrlFormatArtifactCacher implements ArtifactCacher {
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlFormatArtifactCacher.class);

    public static final class AuthConfiguration {
        public final String user;
        public final String pass;

        public AuthConfiguration(String user, String pass) {
            this.user = user;
            this.pass = pass;
        }
    }

    public static final class PutConfiguration {
        public final String md5sumHeader;
        public final String sha1sumHeader;

        public PutConfiguration(String md5sumHeader, String sha1sumHeader) {
            this.md5sumHeader = md5sumHeader;
            this.sha1sumHeader = sha1sumHeader;
        }
    }

    private final String putBaseUrl;
    private final String getBaseUrl;
    private final String archIndependentFormat;
    private final String archDependentFormat;
    private final PutConfiguration putConfiguration;
    private final AuthConfiguration authConfiguration;

    public UrlFormatArtifactCacher(String putBaseUrl, String getBaseUrl, String archIndependentFormat, String archDependentFormat) {
        this(putBaseUrl, getBaseUrl, archIndependentFormat, archDependentFormat, null);
    }

    public UrlFormatArtifactCacher(String putBaseUrl, String getBaseUrl, String archIndependentFormat, String archDependentFormat, PutConfiguration putConfiguration) {
        this(putBaseUrl, getBaseUrl, archIndependentFormat, archDependentFormat, putConfiguration, null);
    }

    public UrlFormatArtifactCacher(String putBaseUrl, String getBaseUrl, String archIndependentFormat, String archDependentFormat, PutConfiguration putConfiguration, AuthConfiguration authConfiguration) {
        if(archIndependentFormat.contains("%a")) {
            throw new IllegalArgumentException("URL arch-independent format should not contain architecture (%a), but does: " + archIndependentFormat);
        }
        if(!archDependentFormat.contains("%a")) {
            throw new IllegalArgumentException("URL arch-dependent format does not contain architecture (%a), but should: " + archDependentFormat);
        }
        this.putBaseUrl = putBaseUrl;
        this.getBaseUrl = getBaseUrl;
        this.archIndependentFormat = archIndependentFormat;
        this.archDependentFormat = archDependentFormat;
        this.putConfiguration = putConfiguration;
        this.authConfiguration = authConfiguration;
    }

    private String url(final String baseUrl, Architecture arch, final CumulativeVersionDigest key) {
        return arch.visit(new Architecture.Visitor<String>() {
            @Override
            public String visitUnknown() {
                return null;
            }

            @Override
            public String visitIndependent() {
                return baseUrl + archIndependentFormat.replace("%v", key.getRawDigest().toString());
            }

            @Override
            public String visitNormal(String arch) {
                return baseUrl + archDependentFormat.replace("%a", arch).replace("%v", key.getRawDigest().toString());
            }
        });
    }

    @Override
    public Pair<Architecture, ArtifactReference> get(final FreeScope scope, final CumulativeVersionDigest key, final Architecture arch) {
        final String url = url(getBaseUrl, arch, key);
        if(url == null) {
            return null;
        }
        try {
            return request(new HttpGet(url), (httpResponse) -> {
                try(QbtTempDir tempDir = new QbtTempDir()) {
                    Path tempFile = tempDir.resolve(key + ".tar.gz");
                    try(InputStream is = httpResponse.getEntity().getContent(); OutputStream os = QbtUtils.openWrite(tempFile)) {
                        ByteStreams.copy(is, os);
                    }
                    ArtifactReference ret = ArtifactReferences.copyFile(scope, tempFile, false);
                    LOGGER.debug("Cache check for " + key + " at " + url + " " + (ret == null ? "missed" : "hit"));
                    return ret == null ? null : Pair.of(arch, ret);
                }
                catch(IOException e) {
                    throw ExceptionUtils.commute(e);
                }
            });
        }
        catch(RuntimeException e) {
            // from here we can't tell errors apart so swallow them all (specifically want to swallow 404)
            LOGGER.debug("Cache check for " + key + " at " + url + " missed", e);
            return null;
        }
    }

    @Override
    public void touch(CumulativeVersionDigest key, Architecture arch) {
    }

    @Override
    public Pair<Architecture, ArtifactReference> intercept(FreeScope scope, CumulativeVersionDigest key, Pair<Architecture, ArtifactReference> p) {
        simplePut(p.getLeft(), key, p.getRight());
        return p;
    }

    private void simplePut(Architecture arch, final CumulativeVersionDigest key, ArtifactReference artifact) {
        final String url = url(putBaseUrl, arch, key);
        if(url == null) {
            return;
        }
        if(putConfiguration == null) {
            return;
        }
        try(QbtTempDir tempDir = new QbtTempDir()) {
            Path tarball = tempDir.resolve("artifact.tar.gz");
            artifact.materializeTarball(tarball);
            HttpPut httpPut = new HttpPut(url);
            if(putConfiguration.md5sumHeader != null) {
                httpPut.addHeader(putConfiguration.md5sumHeader, QbtHashUtils.hash(tarball, Hashing.md5()).toString());
            }
            if(putConfiguration.sha1sumHeader != null) {
                httpPut.addHeader(putConfiguration.sha1sumHeader, QbtHashUtils.hash(tarball, Hashing.sha1()).toString());
            }
            httpPut.setEntity(new FileEntity(tarball.toFile()));
            request(httpPut, (httpResponse) -> {
                LOGGER.debug("Cache put for " + key + " at " + url + " succeeded.");
                return null;
            });
        }
        catch(Exception e) {
            LOGGER.warn("Cache put for " + key + " at " + url + " failed", e);
        }
    }

    @Override
    public void cleanup() {
    }

    protected HttpClientBuilder clientBuilder() {
        HttpClientBuilder b = HttpClients.custom();
        if(authConfiguration != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(authConfiguration.user, authConfiguration.pass));
            b = b.setDefaultCredentialsProvider(credsProvider);
        }
        return b;
    }

    public <T> T request(HttpUriRequest req, Function<HttpResponse, T> cb) {
        try(CloseableHttpClient httpClient = clientBuilder().build()) {
            HttpResponse res = httpClient.execute(req);
            if(res.getStatusLine().getStatusCode() >= 300) {
                throw new RuntimeException(req.getRequestLine() + ": " + res.getStatusLine());
            }
            return cb.apply(res);
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }
}
