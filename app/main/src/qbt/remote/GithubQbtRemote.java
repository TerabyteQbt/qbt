package qbt.remote;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import misc1.commons.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
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

    private String formatUser(RepoTip repoTip) {
        return user.replace("%r", repoTip.name).replace("%t", repoTip.tip);
    }

    private String formatRepo(RepoTip repoTip) {
        return repo.replace("%r", repoTip.name).replace("%t", repoTip.tip);
    }

    private String formatRemote(RepoTip repoTip) {
        return GITHUB_CLONE_URL_PREFIX + formatUser(repoTip) + "/" + formatRepo(repoTip);
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        if(!vcs.remoteExists(remote)) {
            if(!autoVivify) {
                return null;
            }
            autoVivifyRepo(repo);
        }
        return new RawRemote(remote, vcs);
    }

    private void autoVivifyRepo(RepoTip repoTip) {
        String formattedRemote = formatRemote(repoTip);
        String formattedUser = formatUser(repoTip);
        String formattedRepo = formatRepo(repoTip);

        LOGGER.info("Attempting to autovivify github repo " + formattedRemote);

        // API reference: https://developer.github.com/v3/repos/#create
        //
        // Create API requires us to know if a name is a user or an organization.
        // gives 404 if organization not found => it's a user
        HttpResponse orgResponse = request(new HttpGet(GITHUB_API_URL_PREFIX + "orgs/" + formattedUser));

        HttpPost post;
        if(orgResponse.getStatusLine().getStatusCode() < 300) {
            LOGGER.info("Creating github repository for organization " + formattedUser);
            post = new HttpPost(GITHUB_API_URL_PREFIX + "/orgs/" + formattedUser + "/repos");
        }
        else {
            LOGGER.info("Creating github repository for user " + formattedUser);
            post = new HttpPost(GITHUB_API_URL_PREFIX + "/user/repos");
        }

        if(authToken != null) {
            post.addHeader("Authorization", "token " + authToken);
        }
        ImmutableMap<String, String> params = ImmutableMap.of("name", formattedRepo);

        try {
            post.setEntity(new StringEntity(new Gson().toJson(params)));
        }
        catch(UnsupportedEncodingException e1) {
            throw ExceptionUtils.commute(e1);
        }

        HttpResponse createResponse = request(post);
        if(createResponse.getStatusLine().getStatusCode() >= 300) {
            String response;
            try {
                response = new String(ByteStreams.toByteArray(createResponse.getEntity().getContent()));
                // important to tell the user why the request failed if possible, i.e. 401 bad credentials
                throw new RuntimeException("github request failed (" + createResponse.getStatusLine().toString() + "): " + response);
            }
            catch(IllegalStateException | IOException e) {
                throw new RuntimeException("github request failed (" + createResponse.getStatusLine().toString() + "): unknown response");
            }
        }
    }

    private HttpResponse request(HttpUriRequest req) {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().build();
            try {
                return httpClient.execute(req);
            }
            finally {
                httpClient.close();
            }
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }
}
