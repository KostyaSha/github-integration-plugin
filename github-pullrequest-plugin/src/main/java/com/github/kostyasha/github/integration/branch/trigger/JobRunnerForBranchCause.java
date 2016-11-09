package com.github.kostyasha.github.integration.branch.trigger;

import com.github.kostyasha.github.integration.branch.GitHubBranchBadgeAction;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import org.kohsuke.github.GHCommitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cloudbees.jenkins.GitHubWebHook.getJenkinsInstance;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.CAUSE_SKIP;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.FULL_REF;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.HEAD_SHA;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.NAME;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.SHORT_DESC;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.TITLE;
import static com.github.kostyasha.github.integration.branch.data.GitHubBranchEnv.URL;
import static com.github.kostyasha.github.integration.generic.GitHubRepoEnv.GIT_URL;
import static com.github.kostyasha.github.integration.generic.GitHubRepoEnv.SSH_URL;
import static com.google.common.base.Predicates.instanceOf;
import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getDefaultParametersValues;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForBranchCause implements Predicate<GitHubBranchCause> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForBranchCause.class);

    private Job<?, ?> job;
    private GitHubBranchTrigger trigger;

    public JobRunnerForBranchCause(Job<?, ?> job, GitHubBranchTrigger trigger) {
        this.job = job;
        this.trigger = trigger;
    }

    @Override
    public boolean apply(GitHubBranchCause cause) {
        try {
            cause.setPollingLogFile(trigger.getPollingLogAction().getPollingLogFile());

            StringBuilder sb = new StringBuilder();
            sb.append("Jenkins queued the run (").append(cause.getReason()).append(")");

            if (trigger.isCancelQueued() && cancelQueuedBuildByBranchName(cause.getBranchName())) {
                sb.append(". Queued builds aborted");
            }

            QueueTaskFuture<?> queueTaskFuture = startJob(cause);
            if (isNull(queueTaskFuture)) {
                LOGGER.error("{} job didn't start", job.getFullName());
            }

            LOGGER.info(sb.toString());

            // remote connection
            if (trigger.isPreStatus()) {
                trigger.getRemoteRepository()
                        .createCommitStatus(cause.getCommitSha(),
                                GHCommitState.PENDING,
                                null,
                                sb.toString(),
                                job.getFullName());
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
    private static boolean cancelQueuedBuildByBranchName(final String branch) {
        Queue queue = getJenkinsInstance().getQueue();

        for (Queue.Item item : queue.getApproximateItemsQuickly()) {
            Optional<Cause> cause = from(item.getAllActions())
                    .filter(instanceOf(CauseAction.class))
                    .transformAndConcat(new CausesFromAction())
                    .filter(instanceOf(GitHubBranchCause.class))
                    .firstMatch(new CauseHasBranch(branch));

            if (cause.isPresent()) {
                queue.cancel(item);
                return true;
            }
        }

        return false;
    }

    private QueueTaskFuture<?> startJob(GitHubBranchCause cause) {
        ParametersAction parametersAction;
        List<ParameterValue> parameters = getDefaultParametersValues(job);
        final List<ParameterValue> pluginParameters = asList(
                //GitHubBranchEnv
                NAME.param(cause.getBranchName()),
                SHORT_DESC.param(cause.getShortDescription()),
                URL.param(cause.getHtmlUrl().toString()),
                HEAD_SHA.param(cause.getCommitSha()),
                CAUSE_SKIP.param(cause.isSkip()),
                FULL_REF.param(cause.getFullRef()),
                TITLE.param(cause.getTitle()),
                //GitHubRepoEnv
                GIT_URL.param(cause.getGitUrl()),
                SSH_URL.param(cause.getSshUrl())
        );
        parameters.addAll(pluginParameters);

        try {
            Constructor<ParametersAction> constructor = ParametersAction.class.getConstructor(List.class, Collection.class);
            Set<String> names = new HashSet<>();
            for (ParameterValue param : parameters) {
                names.add(param.getName());
            }
            parametersAction = constructor.newInstance(parameters, names);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException
                | InvocationTargetException ex) {
            parametersAction = new ParametersAction(parameters);
        }

        GitHubBranchBadgeAction gitHubBadgeAction = new GitHubBranchBadgeAction(cause);

        //TODO no way to get quietPeriod, so temporary ignore it
        return asParameterizedJobMixIn(job).scheduleBuild2(0,
                new CauseAction(cause),
                parametersAction,
                gitHubBadgeAction);
    }

    private static class CausesFromAction implements Function<Action, Iterable<Cause>> {
        @Override
        public Iterable<Cause> apply(Action input) {
            return ((CauseAction) input).getCauses();
        }
    }

    private static class CauseHasBranch implements Predicate<Cause> {
        private final String branch;

        CauseHasBranch(String branch) {
            this.branch = branch;
        }

        @Override
        public boolean apply(Cause cause) {
            return ((GitHubBranchCause) cause).getBranchName().equals(branch);
        }
    }
}
