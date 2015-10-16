package qbt.remote;

import com.google.common.base.Function;
import misc1.commons.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

public final class GithubQbtRemote implements QbtRemote {
    private static final Logger LOGGER = LoggerFactory.getLogger(GithubQbtRemote.class);

    private final RawRemoteVcs vcs;
    private final String authToken;
    private final String user;
    private final String repo;

    private static final String GITHUB_API_URL_PREFIX = "https://api.github.com/";
    private static final String GITHUB_CLONE_URL_PREFIX = "git@github.com:";

    public GithubQbtRemote(RawRemoteVcs vcs, String authToken, String user) {
        this(vcs, authToken, user, "%r");
    }

    public GithubQbtRemote(RawRemoteVcs vcs, String authToken, String user, String repo) {
        this.vcs = vcs;
        this.authToken = authToken;
        this.user = user;
        this.repo = repo;
    }

    private String formatRemote(RepoTip repoTip) {
        String formattedUser = user.replace("%r", repoTip.name).replace("%t", repoTip.tip);
        String formattedRepo = repo.replace("%r", repoTip.name).replace("%t", repoTip.tip);

        return GITHUB_CLONE_URL_PREFIX + formattedUser + "/" + formattedRepo;
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        if(!vcs.remoteExists(remote)) {
            if(!autoVivify) {
                return null;
            }
            autoVivifyRepo(remote);
        }
        return new RawRemote(remote, vcs);
    }

    private void autoVivifyRepo(String remote) {
        LOGGER.debug("Attempting to autovivify github repo " + remote);

        // orgs API will tell us if user is a regular user or org, which we need to know
        // gives 404 if org not found => it's a user
        boolean isOrg = requestSuccessful(new HttpGet(GITHUB_API_URL_PREFIX + "orgs/" + user));

        LOGGER.debug("XXX User isOrg: " + String.valueOf(isOrg));

        // Create API requires us to know if a name is a user or an org.

        // API reference: https://developer.github.com/v3/repos/#create

    }

    private <T> T request(HttpUriRequest req, Function<HttpResponse, T> cb) {
        try {
            CloseableHttpClient httpClient = clientBuilder().build();
            try {
                HttpResponse res = httpClient.execute(req);
                if(res.getStatusLine().getStatusCode() >= 300) {
                    throw new RuntimeException(req.getRequestLine() + ": " + res.getStatusLine());
                }
                return cb.apply(res);
            }
            finally {
                httpClient.close();
            }
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }

    private boolean requestSuccessful(HttpUriRequest req) {
        try {
            CloseableHttpClient httpClient = clientBuilder().build();
            try {
                HttpResponse res = httpClient.execute(req);
                if(res.getStatusLine().getStatusCode() >= 300) {
                    return false;
                }
                return true;
            }
            finally {
                httpClient.close();
            }
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }
    protected HttpClientBuilder clientBuilder() {
        HttpClientBuilder b = HttpClients.custom();
        //if(authToken != null) {
        //    CredentialsProvider credsProvider = new BasicCredentialsProvider();
        //    credsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(authConfiguration.user, authConfiguration.pass));
        //    b = b.setDefaultCredentialsProvider(credsProvider);
        //}
        return b;
    }
}
