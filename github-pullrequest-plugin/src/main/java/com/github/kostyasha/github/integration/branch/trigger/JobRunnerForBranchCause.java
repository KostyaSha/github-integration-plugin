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
import jenkins.model.ParameterizedJobMixIn;
import org.apache.commons.lang3.reflect.FieldUtils;
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
import static com.google.common.base.Predicates.instanceOf;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getDefaultParametersValues;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForBranchCause implements Predicate<GitHubBranchCause> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForBranchCause.class);

    private Job job;
    private GitHubBranchTrigger trigger;

    public JobRunnerForBranchCause(Job job, GitHubBranchTrigger trigger) {
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

    public QueueTaskFuture<?> startJob(GitHubBranchCause cause) {
        return startJob(cause, null);
    }

    public QueueTaskFuture<?> startJob(GitHubBranchCause cause, Cause additionalCause) {
        ParametersAction parametersAction;
        List<ParameterValue> parameters = getDefaultParametersValues(job);
        cause.fillParameters(parameters);

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

        ParameterizedJobMixIn parameterizedJobMixIn = asParameterizedJobMixIn(job);

        // remove after switch to newer core
        int quietPeriod = 0;
        try {
            Object mixinJob = FieldUtils.readField(parameterizedJobMixIn, "val$job", true);
            ParameterizedJobMixIn.ParameterizedJob parameterizedJob = (ParameterizedJobMixIn.ParameterizedJob) mixinJob;
            quietPeriod = parameterizedJob.getQuietPeriod();
        } catch (IllegalAccessException e) {
            LOGGER.error("Couldn't extract quiet period, falling back to {}", quietPeriod, e);
        }

        CauseAction causeAction;
        if (nonNull(additionalCause)) {
            causeAction = new CauseAction(cause, additionalCause);
        } else {
            causeAction = new CauseAction(cause);
        }

        return asParameterizedJobMixIn(job).scheduleBuild2(quietPeriod,
                causeAction,
                parametersAction,
                gitHubBadgeAction
        );
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
