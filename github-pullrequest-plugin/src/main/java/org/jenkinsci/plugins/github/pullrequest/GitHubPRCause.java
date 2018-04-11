package org.jenkinsci.plugins.github.pullrequest;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.generic.GitHubRepository;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;

import hudson.model.ParameterValue;
import hudson.model.Run;
import jenkins.scm.api.SCMSourceOwner;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

public class GitHubPRCause extends GitHubCause<GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRCause.class);

    private String headSha;
    private int number;
    private boolean mergeable;
    private String targetBranch;
    private String sourceBranch;
    private String prAuthorEmail;
    private String body;

    private String sourceRepoOwner;
    private String triggerSenderName = "";
    private String triggerSenderEmail = "";
    private Set<String> labels;
    /**
     * In case triggered because of commit. See
     * {@link org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent}
     */
    private String commitAuthorName;
    private String commitAuthorEmail;

    private String condRef;
    private String state;
    private String commentBody;
    private String commentBodyMatch;

    public GitHubPRCause() {}

    public GitHubPRCause(GHPullRequest remotePr,
                         GitHubPRRepository localRepo,
                         String reason,
                         boolean skip) {
        this(new GitHubPRPullRequest(remotePr), remotePr.getUser(), localRepo, skip, reason);
        withRemoteData(remotePr);
        if (localRepo != null) {
            withLocalRepo(localRepo);
        }
    }

    @Deprecated
    public GitHubPRCause(GHPullRequest remotePr,
            String reason,
            boolean skip) {
        this(remotePr, null, reason, skip);
    }

    public GitHubPRCause(GitHubPRPullRequest pr,
            GHUser triggerSender,
            GitHubPRRepository localRepo,
            boolean skip,
            String reason) {
        this(pr.getHeadSha(), pr.getNumber(),
                pr.isMergeable(), pr.getBaseRef(), pr.getHeadRef(),
                pr.getUserEmail(), pr.getTitle(), pr.getHtmlUrl(), pr.getSourceRepoOwner(),
                pr.getLabels(),
                triggerSender, skip, reason, "", "", pr.getState());
        this.body = pr.getBody();
        if(localRepo != null) {
            withLocalRepo(localRepo);
        }
    }

    @Deprecated
    public GitHubPRCause(GitHubPRPullRequest pr,
            GHUser triggerSender,
            boolean skip,
            String reason) {
        this(pr, triggerSender, null, skip, reason);
    }

    // FIXME (sizes) ParameterNumber: More than 7 parameters (found 15).
    // CHECKSTYLE:OFF
    public GitHubPRCause(String headSha, int number, boolean mergeable,
            String targetBranch, String sourceBranch, String prAuthorEmail,
            String title, URL htmlUrl, String sourceRepoOwner, Set<String> labels,
            GHUser triggerSender, boolean skip, String reason,
            String commitAuthorName, String commitAuthorEmail,
            String state) {
        // CHECKSTYLE:ON
        this.headSha = headSha;
        this.number = number;
        this.mergeable = mergeable;
        this.targetBranch = targetBranch;
        this.sourceBranch = sourceBranch;
        this.prAuthorEmail = prAuthorEmail;
        withTitle(title);
        withHtmlUrl(htmlUrl);
        this.sourceRepoOwner = sourceRepoOwner;
        this.labels = labels;
        withSkip(skip);
        withReason(reason);
        this.commitAuthorName = commitAuthorName;
        this.commitAuthorEmail = commitAuthorEmail;

        if (nonNull(triggerSender)) {
            try {
                this.triggerSenderName = triggerSender.getName();
            } catch (IOException e) {
                LOGGER.error("Can't get trigger sender name from remote PR");
            }

            try {
                this.triggerSenderEmail = triggerSender.getEmail();
            } catch (IOException e) {
                LOGGER.error("Can't get trigger sender email from remote PR");
            }
        }

        this.condRef = mergeable ? "merge" : "head";

        this.state = state;
    }

    @Override
    public GitHubPRCause withLocalRepo(@Nonnull GitHubRepository localRepo) {
        withGitUrl(localRepo.getGitUrl());
        withSshUrl(localRepo.getSshUrl());
        // html url is set from constructor and points to pr
        // withHtmlUrl(localRepo.getGithubUrl());
        return this;
    }

    /**
     * Copy constructor
     */
    public GitHubPRCause(GitHubPRCause orig) {
        this(orig.getHeadSha(), orig.getNumber(), orig.isMergeable(),
                orig.getTargetBranch(), orig.getSourceBranch(),
                orig.getPRAuthorEmail(), orig.getTitle(),
                orig.getHtmlUrl(), orig.getSourceRepoOwner(), orig.getLabels(), null,
                orig.isSkip(), orig.getReason(), orig.getCommitAuthorName(), orig.getCommitAuthorEmail(), orig.getState());
        withTriggerSenderName(orig.getTriggerSenderEmail());
        withTriggerSenderEmail(orig.getTriggerSenderEmail());
        withBody(orig.getBody());
        withCommentBody(orig.getCommentBody());
        withCommentBodyMatch(orig.getCommentBodyMatch());
        withCommitAuthorName(orig.getCommitAuthorName());
        withCommitAuthorEmail(orig.getCommitAuthorEmail());
        withCondRef(orig.getCondRef());
        withGitUrl(orig.getGitUrl());
        withSshUrl(orig.getSshUrl());
        withPollingLog(orig.getPollingLog());
    }

    public static GitHubPRCause newGitHubPRCause() {
        return new GitHubPRCause();
    }

    /**
     * @see #headSha
     */
    public GitHubPRCause withHeadSha(String headSha) {
        this.headSha = headSha;
        return this;
    }

    /**
     * @see #number
     */
    public GitHubPRCause withNumber(int number) {
        this.number = number;
        return this;
    }

    /**
     * @see #mergeable
     */
    public GitHubPRCause withMergeable(boolean mergeable) {
        this.mergeable = mergeable;
        return this;
    }

    /**
     * @see #targetBranch
     */
    public GitHubPRCause withTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
        return this;
    }

    /**
     * @see #sourceBranch
     */
    public GitHubPRCause withSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
        return this;
    }

    /**
     * @see #prAuthorEmail
     */
    public GitHubPRCause withPrAuthorEmail(String prAuthorEmail) {
        this.prAuthorEmail = prAuthorEmail;
        return this;
    }

    /**
     * @see #sourceRepoOwner
     */
    public GitHubPRCause withSourceRepoOwner(String sourceRepoOwner) {
        this.sourceRepoOwner = sourceRepoOwner;
        return this;
    }

    /**
     * @see #triggerSenderName
     */
    public GitHubPRCause withTriggerSenderName(String triggerSenderName) {
        this.triggerSenderName = triggerSenderName;
        return this;
    }

    /**
     * @see #triggerSenderEmail
     */
    public GitHubPRCause withTriggerSenderEmail(String triggerSenderEmail) {
        this.triggerSenderEmail = triggerSenderEmail;
        return this;
    }

    /**
     * @see #labels
     */
    public GitHubPRCause withLabels(Set<String> labels) {
        this.labels = labels;
        return this;
    }

    /**
     * @see #commitAuthorName
     */
    public GitHubPRCause withCommitAuthorName(String commitAuthorName) {
        this.commitAuthorName = commitAuthorName;
        return this;
    }

    /**
     * @see #commitAuthorEmail
     */
    public GitHubPRCause withCommitAuthorEmail(String commitAuthorEmail) {
        this.commitAuthorEmail = commitAuthorEmail;
        return this;
    }

    /**
     * @see #condRef
     */
    public GitHubPRCause withCondRef(String condRef) {
        this.condRef = condRef;
        return this;
    }

    public GitHubPRCause withCommentBody(String commentBody) {
        this.commentBody = commentBody;
        return this;
    }

    public GitHubPRCause withCommentBodyMatch(String commentBodyMatch) {
        this.commentBodyMatch = commentBodyMatch;
        return this;
    }

    public String getBody() {
        return body;
    }

    public GitHubPRCause withBody(String body) {
        this.body = body;
        return this;
    }

    @Override
    public String getShortDescription() {
        return "GitHub PR #<a href=\"" + getHtmlUrl() + "\">" + number + "</a>, " + getReason();
    }

    public String getHeadSha() {
        return headSha;
    }

    public boolean isMergeable() {
        return mergeable;
    }

    public int getNumber() {
        return number;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getPRAuthorEmail() {
        return prAuthorEmail;
    }

    public String getSourceRepoOwner() {
        return sourceRepoOwner;
    }

    @Nonnull
    public Set<String> getLabels() {
        return isNull(labels) ? Collections.<String>emptySet() : labels;
    }

    public String getTriggerSenderName() {
        return triggerSenderName;
    }

    public String getTriggerSenderEmail() {
        return triggerSenderEmail;
    }

    public String getPrAuthorEmail() {
        return prAuthorEmail;
    }

    public String getCommitAuthorName() {
        return commitAuthorName;
    }

    public String getCommitAuthorEmail() {
        return commitAuthorEmail;
    }

    public String getState() {
        return state;
    }

    @Nonnull
    public String getCondRef() {
        return condRef;
    }

    /**
     * When trigger by comment, body of comment.
     */
    public String getCommentBody() {
        return commentBody;
    }

    /**
     * When trigger by comment, string matched to pattern.
     */
    public String getCommentBodyMatch() {
        return commentBodyMatch;
    }

    @Override
    public void fillParameters(List<ParameterValue> params) {
        GitHubPREnv.getParams(this, params);
    }

    @Override
    public GitHubSCMHead<GitHubPRCause> createSCMHead(String sourceId) {
        return new GitHubPRSCMHead(number, targetBranch, sourceId);
    }

    @Override
    public void onAddedTo(@Nonnull Run run) {
        if (run.getParent().getParent() instanceof SCMSourceOwner) {
            // skip multibranch
            return;
        }

        // move polling log from cause to action
        try {
            GitHubPRPollingLogAction action = new GitHubPRPollingLogAction(run);
            FileUtils.writeStringToFile(action.getPollingLogFile(), getPollingLog());
            run.replaceAction(action);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist the polling log", e);
        }
        setPollingLog(null);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
