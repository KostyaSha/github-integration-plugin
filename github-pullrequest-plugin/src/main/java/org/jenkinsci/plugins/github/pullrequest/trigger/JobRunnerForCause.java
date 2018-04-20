package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.model.queue.SubTask;
import hudson.security.ACL;
import jenkins.model.ParameterizedJobMixIn;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRBadgeAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHCommitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.cloudbees.jenkins.GitHubWebHook.getJenkinsInstance;
import static com.google.common.base.Predicates.instanceOf;
import static hudson.security.ACL.impersonate;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getDefaultParametersValues;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getInterruptCauses;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.getInterruptStatus;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;


/**
 * @author lanwen (Merkushev Kirill)
 */
public class JobRunnerForCause implements Predicate<GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForCause.class);

    private Job<?, ?> job;
    private GitHubPRTrigger trigger;

    public JobRunnerForCause(Job<?, ?> job, GitHubPRTrigger trigger) {
        this.job = job;
        this.trigger = trigger;
    }

    @Override
    public boolean apply(final GitHubPRCause cause) {
        SecurityContext old = ACL.impersonate(ACL.SYSTEM);

        try {
            cause.setPollingLogFile(trigger.getPollingLogAction().getPollingLogFile());

            StringBuilder sb = new StringBuilder();
            sb.append("Jenkins queued the run (").append(cause.getReason()).append(")");

            if (trigger.isCancelQueued()) {
                int i = cancelQueuedBuildByPrNumber(cause.getNumber());
                if (i > 0) {
                    sb.append(". ");
                    sb.append(i);
                    sb.append(" queued builds/runs canceled.");
                }
            }

            if (trigger.isAbortRunning()) {
                int i = 0;
                try {
                    i = abortRunning(cause.getNumber());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Can't abort runs/builds for {}", job.getFullName(), e);
                }
                if (i > 0) {
                    sb.append(". ");
                    sb.append(i);
                    sb.append(" running builds/runs aborted.");
                }
            }

            QueueTaskFuture<?> queueTaskFuture = startJob(cause);
            if (isNull(queueTaskFuture)) {
                LOGGER.error("{} job didn't start", job.getFullName());
            }

            LOGGER.info(sb.toString());

            // remote connection
            if (trigger.isPreStatus()) {
                if (job instanceof MatrixProject) {
                    Collection<? extends MatrixConfiguration> configs = ((MatrixProject) job).getActiveConfigurations();
                    for (MatrixConfiguration config : configs) {
                        trigger.getRemoteRepository()
                                .createCommitStatus(cause.getHeadSha(),
                                        GHCommitState.PENDING,
                                        config.getAbsoluteUrl(),
                                        sb.toString(),
                                        config.getFullName());
                    }
                } else {
                    trigger.getRemoteRepository()
                            .createCommitStatus(cause.getHeadSha(),
                                    GHCommitState.PENDING,
                                    job.getAbsoluteUrl(),
                                    sb.toString(),
                                    job.getFullName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Can't trigger build:", e);
            return false;
        } finally {
            SecurityContextHolder.setContext(old);
        }
        return true;
    }

    public synchronized int abortRunning(int number) throws IllegalAccessException {
        int aborted = 0;

        Computer[] computers = getJenkinsInstance().getComputers();
        for (Computer computer : computers) {
            if (isNull(computer)) {
                continue;
            }

            List<Executor> executors = computer.getExecutors();
            executors.addAll(computer.getOneOffExecutors());

            for (Executor executor : executors) {
                if (isNull(executor) || !executor.isBusy() || nonNull(executor.getCauseOfDeath()) ||
                        !getInterruptCauses(executor).isEmpty() || getInterruptStatus(executor) == Result.ABORTED) {
                    continue;
                }

                Queue.Executable executable = executor.getCurrentExecutable();
                final SubTask parent = executable.getParent();

                if (!(executable instanceof Run)) {
                    continue;
                }
                final Run executableRun = (Run) executable;

                if (!(parent instanceof Job)) {
                    continue;
                }
                final Job parentJob = (Job) parent;

                if (!parentJob.getFullName().equals(job.getFullName())) {
                    // name doesn't match
                    continue;
                }

                if (executableRun.getResult() == Result.ABORTED) {
                    // was already aborted
                    continue;
                }

                if (executableRun instanceof MatrixRun) {
                    // the whole MatrixBuild will be aborted
                    continue;
                }
//                if (executable instanceof MatrixBuild) {
//                    final MatrixBuild executable1 = (MatrixBuild) executable;
//                    executable1.doStop()
//                }

                final GitHubPRCause causeAction = (GitHubPRCause) executableRun.getCause(GitHubPRCause.class);
                if (nonNull(causeAction) && causeAction.getNumber() == number) {
                    LOGGER.info("Aborting '{}', by interrupting '{}'", executableRun, executor);
                    executor.interrupt(Result.ABORTED, new NewPRInterruptCause());
                    aborted++;
                }
            }
        }

        return aborted;
    }

    /**
     * Cancel previous builds for specified PR id.
     */
    public int cancelQueuedBuildByPrNumber(final int id) {
        int canceled = 0;
        SecurityContext old = impersonate(ACL.SYSTEM);
        try {
            final Queue queue = getJenkinsInstance().getQueue();
            final Queue.Item[] items = queue.getItems();

            //todo replace with stream?
            for (Queue.Item item : items) {
                if (!(item.task instanceof Job)) {
                    LOGGER.debug("Item {} not instanceof job", item);
                    continue;
                }

                final Job<?, ?> jobTask = (Job<?, ?>) item.task;
                if (!jobTask.getFullName().equals(job.getFullName())) {
                    LOGGER.debug("{} != {}", jobTask.getFullName(), job.getFullName());
                    continue;
                }

                final CauseAction action = item.getAction(CauseAction.class);
                if (isNull(action)) {
                    LOGGER.debug("Cause action is null for {}", jobTask.getFullName());
                    continue;
                }

                Optional<Cause> cause = from(action.getCauses())
                        .filter(instanceOf(GitHubPRCause.class))
                        .firstMatch(new CauseHasPRNum(id));

                if (cause.isPresent()) {
                    LOGGER.debug("Cancelling {}", item);
                    queue.cancel(item);
                    canceled++;
                }
            }
        } finally {
            SecurityContextHolder.setContext(old);
        }

        return canceled;
    }

    public QueueTaskFuture<?> startJob(GitHubPRCause cause) {
        return startJob(cause, null);
    }

    public QueueTaskFuture<?> startJob(GitHubPRCause cause, @CheckForNull Cause additionalCause) {
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

        GitHubPRBadgeAction gitHubPRBadgeAction = new GitHubPRBadgeAction(cause);

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

        return parameterizedJobMixIn.scheduleBuild2(
                quietPeriod,
                causeAction,
                parametersAction,
                gitHubPRBadgeAction
        );
    }

    protected static class CausesFromAction implements Function<Action, Iterable<Cause>> {
        @Override
        public Iterable<Cause> apply(Action input) {
            return ((CauseAction) input).getCauses();
        }
    }

    protected static class CauseHasPRNum implements Predicate<Cause> {
        private final int id;

        CauseHasPRNum(int id) {
            this.id = id;
        }

        @Override
        public boolean apply(Cause cause) {
            return ((GitHubPRCause) cause).getNumber() == id;
        }
    }

}
