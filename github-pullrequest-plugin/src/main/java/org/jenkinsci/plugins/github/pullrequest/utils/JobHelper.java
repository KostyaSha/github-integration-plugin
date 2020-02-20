package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.AbortException;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.triggers.Trigger;
import jenkins.model.CauseOfInterruption;
import jenkins.model.ParameterizedJobMixIn;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * @author Kanstantsin Shautsou
 */
public class JobHelper {
    private static final Logger LOG = LoggerFactory.getLogger(JobHelper.class);

    private JobHelper() {
    }

    @CheckForNull
    public static GitHubPRTrigger ghPRTriggerFromRun(Run<?, ?> run) {
        return triggerFrom(run.getParent(), GitHubPRTrigger.class);
    }

    @CheckForNull
    public static GitHubPRTrigger ghPRTriggerFromJob(Job<?, ?> job) {
        return triggerFrom(job, GitHubPRTrigger.class);
    }

    /**
     * support matrix plugin.
     *
     * @see JobInfoHelpers#triggerFrom(hudson.model.Job, java.lang.Class)
     */
    @CheckForNull
    public static <T extends Trigger> T triggerFrom(final Job<?, ?> job, Class<T> tClass) {
        Job<?, ?> guessJob;
        if (job instanceof MatrixConfiguration) {
            guessJob = ((MatrixConfiguration) job).getParent();
        } else {
            guessJob = job;
        }

        if (guessJob instanceof AbstractProject<?, ?>) {
            final AbstractProject<?, ?> abstractProject = (AbstractProject<?, ?>) guessJob;
            return abstractProject.getTrigger(tClass);
        } else if (guessJob instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) guessJob;

            for (Object candidate : pJob.getTriggers().values()) {
                if (tClass.isInstance(candidate)) {
                    return tClass.cast(candidate);
                }
            }
        }
        return null;
    }

    @CheckForNull
    public static GitHubPRCause ghPRCauseFromRun(Run<?, ?> run) {
        return ghCauseFromRun(run, GitHubPRCause.class);
    }

    /**
     * matrix-project requires special extraction.
     */
    @CheckForNull
    public static <T extends Cause> T ghCauseFromRun(Run<?, ?> run, Class<T> tClass) {
        if (run instanceof MatrixRun) {
            MatrixBuild parentBuild = ((MatrixRun) run).getParentBuild();
            if (nonNull(parentBuild)) {
                return parentBuild.getCause(tClass);
            }
        } else {
            return run.getCause(tClass);
        }

        return null;
    }

    public static Result getInterruptStatus(Executor executor) throws IllegalAccessException {
        return (Result) FieldUtils.readField(executor, "interruptStatus", true);
    }

    public static List<CauseOfInterruption> getInterruptCauses(Executor executor) throws IllegalAccessException {
        return (List<CauseOfInterruption>) FieldUtils.readField(executor, "causes", true);
    }

    /**
     * @see jenkins.model.ParameterizedJobMixIn#getDefaultParametersValues()
     */
    public static List<ParameterValue> getDefaultParametersValues(Job<?, ?> job) {
        ParametersDefinitionProperty paramDefProp = job.getProperty(ParametersDefinitionProperty.class);
        List<ParameterValue> defValues = new ArrayList<>();

        /*
         * This check is made ONLY if someone will call this method even if isParametrized() is false.
         */
        if (isNull(paramDefProp)) {
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

    public static boolean rebuild(Run<?, ?> run) {
        final QueueTaskFuture queueTaskFuture = asParameterizedJobMixIn(run.getParent())
                .scheduleBuild2(
                        0,
                        run.getAction(ParametersAction.class),
                        run.getAction(CauseAction.class),
                        run.getAction(BuildBadgeAction.class)
                );
        return queueTaskFuture != null;
    }

    public static GHRepository getGhRepositoryFromPRTrigger(final Run<?, ?> run) throws IOException {
        return ghPRTriggerFromRun(run).getRemoteRepository();
    }

    public static int getPRNumberFromPRCause(final Run<?, ?> run) throws AbortException {
        GitHubPRCause cause = ghPRCauseFromRun(run);
        if (isNull(cause)) {
            throw new AbortException("Can't get cause from run/build");
        }
        return cause.getNumber();
    }

    public static GHIssue getGhIssue(final Run<?, ?> run) throws IOException {
        return getGhRepositoryFromPRTrigger(run).getIssue(getPRNumberFromPRCause(run));
    }

    public static GHIssue getGhPullRequest(final Run<?, ?> run) throws IOException {
        return getGhRepositoryFromPRTrigger(run).getPullRequest(getPRNumberFromPRCause(run));
    }

    public static void addComment(final int id, final String comment, final Run<?, ?> run, final TaskListener listener) {
        if (comment == null || comment.trim().isEmpty()) {
            return;
        }

        String finalComment = comment;
        if (nonNull(run) && nonNull(listener)) {
            try {
                finalComment = run.getEnvironment(listener).expand(comment);
            } catch (Exception e) {
                LOG.error("Error", e);
            }
        }

        try {
            if (nonNull(run)) {
                final GitHubPRTrigger trigger = ghPRTriggerFromRun(run);

                GHRepository ghRepository = trigger.getRemoteRepository();
                ghRepository.getPullRequest(id).comment(finalComment);
            }
        } catch (IOException ex) {
            LOG.error("Couldn't add comment to pull request #{}: '{}'", id, finalComment, ex);
        }
    }

    public static GHCommitState getCommitState(final Run<?, ?> run, final GHCommitState unstableAs) {
        GHCommitState state;
        Result result = run.getResult();
        if (isNull(result)) {
            LOG.error("{} result is null.", run);
            state = GHCommitState.ERROR;
        } else if (result.isBetterOrEqualTo(SUCCESS)) {
            state = GHCommitState.SUCCESS;
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            state = unstableAs;
        } else {
            state = GHCommitState.FAILURE;
        }
        return state;
    }
}
