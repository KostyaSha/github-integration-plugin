package org.jenkinsci.plugins.github.pullrequest;

// import hudson.Functions;
// import org.apache.commons.lang3.builder.EqualsBuilder;
// import org.apache.commons.lang3.builder.HashCodeBuilder;
// import org.apache.commons.lang3.builder.ToStringBuilder;
// import org.kohsuke.github.GHIssueComment;
// import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequestReview;
import org.kohsuke.github.GHPullRequestReviewState;
import org.kohsuke.github.GHPullRequestReviewComment;
import org.kohsuke.github.PagedIterable;
// import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import javax.annotation.CheckForNull;
// import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
// import java.util.Collection;
// import java.util.Collections;
// import java.util.Date;
// import java.util.HashSet;
// import java.util.List;
// import java.util.Set;

// import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
// import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
// import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * Maintains state about a Pull Request Review for a particular Jenkins job.  This is what understands the current state
 * of a PR for a particular job. Instances of this class are immutable.
 * 
 */
public class GitHubPRPullRequestReview {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRPullRequestReview.class);

    private String commitId;
    private String body;
    private GitHubPRPullRequest parent = null; // the PR it refers to
    private String state;
    private URL htmlUrl;
    private PagedIterable<GHPullRequestReviewComment> comments;

    /**
     * Save only what we need for next comparison
     */
    public GitHubPRPullRequestReview(GHPullRequestReview prr) throws IOException {
        body = prr.getBody();
        commitId = prr.getCommitId();
        state = prr.getState();
        htmlUrl = prr.getHtmlUrl();
        parent = new GitHubPRPullRequest(prr.getParent());
    }

    public String getCommitId(){
        return commitId;
    }

    public String getBody(){
        return body;
    }

    public GitHubPRPullRequest getParent(){
        return parent;
    }

    public String getState(){
        return state;
    }

    public URL getURL(){
        return htmlUrl;
    }

    public PagedIterable<GHPullRequestReviewComment> getComments(){
        return comments;
    }
}