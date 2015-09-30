package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger.DescriptorImpl.githubFor;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.AUTHOR_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.CAUSE_SKIP;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COMMIT_AUTHOR_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COMMIT_AUTHOR_NAME;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COND_REF;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.HEAD_SHA;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.NUMBER;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SHORT_DESC;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SOURCE_BRANCH;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SOURCE_REPO_OWNER;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TARGET_BRANCH;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TITLE;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TRIGGER_SENDER_AUTHOR;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TRIGGER_SENDER_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.URL;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class JobRunnerForCause implements Predicate<GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForCause.class);
    private static final Cause NO_CAUSE = null;
    
    private AbstractProject<?, ?> job;
    private GitHubPRTrigger trigger;

    public JobRunnerForCause(AbstractProject<?, ?> job, GitHubPRTrigger trigger) {
        this.job = job;
        this.trigger = trigger;
    }

    @Override
    public boolean apply(GitHubPRCause cause) {
        try {
            cause.setPollingLog(trigger.getPollingLogAction().getLogFile());

            StringBuilder sb = new StringBuilder();
            sb.append("Jenkins queued the run (").append(cause.getReason()).append(")");

            if (trigger.isCancelQueued() && cancelQueuedBuildByPrNumber(cause.getNumber())) {
                sb.append(". Queued builds aborted");
            }

            QueueTaskFuture<?> queueTaskFuture = startJob(cause);
            if (queueTaskFuture == null) {
                LOGGER.error("{} job didn't start", job.getFullName());
            }

            LOGGER.info(sb.toString());

            // remote connection
            GitHub connection = githubFor(URI.create(cause.getHtmlUrl().toString()));
            if (trigger.isPreStatus()) {
                connection.getRepository(trigger.getRepoFullName(job))
                        .createCommitStatus(cause.getHeadSha(),
                                GHCommitState.PENDING,
                                null,
                                sb.toString(),
                                job.getFullName()
                        );
            }
        } catch (IOException e) {
            LOGGER.error("Can't trigger build ({})", e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Cancel previous builds for specified PR id.
     */
    private boolean cancelQueuedBuildByPrNumber(int id) {
        Queue queue = GitHubWebHook.getJenkinsInstance().getQueue();
        List<Queue.Item> approximateItemsQuickly = queue.getApproximateItemsQuickly();

        for (Queue.Item item : approximateItemsQuickly) {
            List<? extends Action> allActions = item.getAllActions();
            for (Action action : allActions) {
                if (action instanceof CauseAction) {
                    CauseAction causeAction = (CauseAction) action;
                    for (Cause cause : causeAction.getCauses()) {
                        if (cause instanceof GitHubPRCause) {
                            GitHubPRCause gitHubPRCause = (GitHubPRCause) cause;
                            if (gitHubPRCause.getNumber() == id) {
                                queue.cancel(item);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private QueueTaskFuture<?> startJob(GitHubPRCause cause) {
        List<ParameterValue> values = getDefaultParametersValues();
        values.addAll(asList(
                TRIGGER_SENDER_AUTHOR.param(cause.getTriggerSenderName()),
                TRIGGER_SENDER_EMAIL.param(cause.getTriggerSenderEmail()),
                COMMIT_AUTHOR_NAME.param(cause.getCommitAuthorName()),
                COMMIT_AUTHOR_EMAIL.param(cause.getCommitAuthorEmail()),
                TARGET_BRANCH.param(cause.getTargetBranch()),
                SOURCE_BRANCH.param(cause.getSourceBranch()),
                AUTHOR_EMAIL.param(cause.getPRAuthorEmail()),
                SHORT_DESC.param(cause.getShortDescription()),
                TITLE.param(cause.getTitle()),
                URL.param(cause.getHtmlUrl().toString()),
                SOURCE_REPO_OWNER.param(cause.getSourceRepoOwner()),
                HEAD_SHA.param(cause.getHeadSha()),
                COND_REF.param(cause.getCondRef()),
                CAUSE_SKIP.param(cause.isSkip()),
                NUMBER.param(String.valueOf(cause.getNumber()))
        ));

        return job.scheduleBuild2(job.getQuietPeriod(), NO_CAUSE,
                asList(new CauseAction(cause), new ParametersAction(values)));
    }

    /**
     * @see jenkins.model.ParameterizedJobMixIn#getDefaultParametersValues()
     */
    private List<ParameterValue> getDefaultParametersValues() {
        ParametersDefinitionProperty paramDefProp = job.getProperty(ParametersDefinitionProperty.class);
        List<ParameterValue> defValues = new ArrayList<>();

        /*
        * This check is made ONLY if someone will call this method even if isParametrized() is false.
        */
        if (paramDefProp == null) {
            return defValues;
        }

        /* Scan for all parameter with an associated default values */
        for (ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

            if (defaultValue != null) {
                defValues.add(defaultValue);
            }
        }

        return defValues;
    }

}
