package qbt.remote;

import java.io.UnsupportedEncodingException;

import misc1.commons.ExceptionUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

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
    	
        LOGGER.debug("Attempting to autovivify github repo " + formattedRemote);

        // orgs API will tell us if user is a regular user or org, which we need to know
        // gives 404 if org not found => it's a user
        boolean isOrg = requestSuccessful(new HttpGet(GITHUB_API_URL_PREFIX + "orgs/" + formattedUser));

        LOGGER.debug("XXX User isOrg: " + String.valueOf(isOrg));

        if(isOrg) {
            LOGGER.debug("Creating github repository for organization " + formattedUser);
            // For an orCreate API requires us to know if a name is a user or an org.
            HttpPost post = new HttpPost(GITHUB_API_URL_PREFIX + "/orgs/" + formattedUser + "/repos");
            ImmutableMap<String, String> params = ImmutableMap.of("name", formattedRepo);
            
            try {
				post.setEntity(new StringEntity(new Gson().toJson(params)));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            boolean created = requestSuccessful(post);
            LOGGER.debug("create returned: " + String.valueOf(created));
        }


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
