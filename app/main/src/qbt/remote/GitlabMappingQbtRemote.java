package qbt.remote;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import misc1.commons.ExceptionUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qbt.tip.RepoTip;
import qbt.vcs.RawRemote;
import qbt.vcs.RawRemoteVcs;

/**
 * This class enables using remotes stored in Gitlab.  Optional features include:
 * * Autovivify repos in Gitlab if not present
 * * Mapping based upon prefixes
 *
 * Because gitlab allows dashes in repo names, and uses slashes for nested repos, those have to be mapped.
 * The defaults are:
 * "-" => "_d_"
 * "/" => "_s_"
 *
 * Example mapping:
 * ImmutableMap.of("^root\\.(.*)\$", "%m", "^(.*)\$", "qbt/%m")
 *
 * This mapping will place all things that start with "root." into the root, e.g. "root.foo_s_bar" becomes repo "foo/bar"
 * Everything else is caught by the last rule e.g. "foo_s_bar_d_baz" becomes "qbt/foo/bar-baz"
 */
public final class GitlabMappingQbtRemote implements QbtRemote {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitlabMappingQbtRemote.class);

    private final RawRemoteVcs vcs;
    private final ImmutableMap<String, String> mappings;
    private final String gitlabApiUrlPrefix;
    private final String gitlabCloneUrlPrefix;
    private final String authToken;
    private final Boolean translateNestedRepos;
    private final String slashSigil;
    private final String dashSigil;

    public GitlabMappingQbtRemote(RawRemoteVcs vcs, ImmutableMap<String, String> mappings, String gitlabApiUrlPrefix, String gitlabCloneUrlPrefix, String authToken, Boolean translateNestedRepos) {
        this(vcs, mappings, gitlabApiUrlPrefix, gitlabCloneUrlPrefix, authToken, translateNestedRepos, "_d_", "_s_");
    }

    public GitlabMappingQbtRemote(RawRemoteVcs vcs, ImmutableMap<String, String> mappings, String gitlabApiUrlPrefix, String gitlabCloneUrlPrefix, String authToken, Boolean translateNestedRepos, String dashSigil, String slashSigil) {
        this.vcs = vcs;
        this.mappings = mappings;
        this.gitlabApiUrlPrefix = gitlabApiUrlPrefix;
        this.gitlabCloneUrlPrefix = gitlabCloneUrlPrefix;
        this.authToken = authToken;
        this.translateNestedRepos = translateNestedRepos;
        this.slashSigil = slashSigil;
        this.dashSigil = dashSigil;
    }

    private String replaceSigils(String in) {
        return in.replace(slashSigil, "/").replace(dashSigil, "-");
    }

    private String formatRepo(RepoTip repoTip) {
        // Did you know ImmutableMap preserves insertion-order?  TIL...
        for(Map.Entry<String, String> e : mappings.entrySet()) {
            Matcher m = Pattern.compile(e.getKey()).matcher(repoTip.name);
            if(m.matches()) {
                return replaceSigils(e.getValue().replace("%m", m.group(1)).replace("%r", repoTip.name).replace("%t", repoTip.tip));
            }
        }
        // no matches, just use repo name as-is substituting slashSigil only
        return replaceSigils(repoTip.name);
    }

    private String formatRemote(RepoTip repoTip) {
        return gitlabCloneUrlPrefix + formatRepo(repoTip);
    }

    @Override
    public RawRemote findRemote(RepoTip repo, boolean autoVivify) {
        String remote = formatRemote(repo);
        if(vcs.remoteExists(remote)) {
            return new RawRemote(remote, vcs);
        }

        if(!autoVivify) {
            // doesn't exist and we weren't asked to create it
            return null;
        }

        autoVivifyRepo(repo);
        return new RawRemote(remote, vcs);
    }

    private void autoVivifyRepo(RepoTip repoTip) {
        String formattedRemote = formatRemote(repoTip);
        String formattedRepo = formatRepo(repoTip);

        LOGGER.info("Attempting to autovivify gitlab repo " + formattedRemote);

        /*
        // API reference: https://developer.github.com/v3/repos/#create
        //
        // Create API requires us to know if a name is a user or an organization.
        // gives 404 if organization not found => it's a user
        HttpGet get = new HttpGet(GITHUB_API_URL_PREFIX + "orgs/" + formattedUser);
        if(authToken != null) {
            get.addHeader("Authorization", "token " + authToken);
        }

        HttpResponse orgResponse = request(get);

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
                throw new RuntimeException("github request failed (" + createResponse.getStatusLine().toString() + "): unknown response", e);
            }
        }
        */
    }

    private HttpResponse request(HttpUriRequest req) {
        try(CloseableHttpClient httpClient = HttpClients.custom().build()) {
            return httpClient.execute(req);
        }
        catch(Exception e) {
            throw ExceptionUtils.commute(e);
        }
    }
}
