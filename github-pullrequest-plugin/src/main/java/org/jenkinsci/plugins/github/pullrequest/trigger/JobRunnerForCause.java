package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Computer;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.queue.QueueTaskFuture;
import hudson.model.queue.SubTask;
import hudson.security.ACL;
import javaposse.jobdsl.dsl.jobs.WorkflowJob;
import jenkins.model.CauseOfInterruption;
import jenkins.model.ParameterizedJobMixIn;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRBadgeAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.utils.JobHelper;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.github.GHCommitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cloudbees.jenkins.GitHubWebHook.getJenkinsInstance;
import static com.google.common.base.Predicates.instanceOf;
import static hudson.security.ACL.impersonate;
import static java.util.Arrays.asList;
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
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
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
            cause.setPollingLog(trigger.getPollingLogAction().getPollingLogFile());

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
                final int i = abortRunning(cause.getNumber());
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
                        trigger.getRemoteRepo()
                                .createCommitStatus(cause.getHeadSha(),
                                        GHCommitState.PENDING,
                                        config.getAbsoluteUrl(),
                                        sb.toString(),
                                        config.getFullName());
                    }
                } else {
                    trigger.getRemoteRepo()
                            .createCommitStatus(cause.getHeadSha(),
                                    GHCommitState.PENDING,
                                    job.getAbsoluteUrl(),
                                    sb.toString(),
                                    job.getFullName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Can't trigger build ({})", e.getMessage(), e);
            return false;
        } finally {
            SecurityContextHolder.setContext(old);
        }
        return true;
    }

    public synchronized int abortRunning(int number) {
        int aborted = 0;

        Computer[] computers = getJenkinsInstance().getComputers();
        for (Computer computer : computers) {
            if (isNull(computer)) {
                continue;
            }

            List<Executor> executors = computer.getExecutors();
            executors.addAll(computer.getOneOffExecutors());

            for (Executor executor : executors) {
                if (isNull(executor)) {
                    continue;
                }

                if (!executor.isBusy()) {
                    continue;
                }

                Queue.Executable executable = executor.getCurrentExecutable();
                final SubTask parent = executable.getParent();

                if (executable instanceof WorkflowRun) {
                    // we want interrupt parent run
                    continue;
                } else if (executable instanceof WorkflowJob) {
                    final WorkflowJob workflowJob = (WorkflowJob) parent;
                } else if (executable instanceof Run) {
                    final Run executableRun = (Run) executable;
                    if (executableRun.getResult() == Result.ABORTED) {
                        continue;
                    }


                    if (parent instanceof Job && ((Job) parent).getFullName().equals(job.getFullName())) {
                        if (parent instanceof WorkflowJob) {

                        } else if (parent instanceof MatrixConfiguration) {

                        } else {
                            final GitHubPRCause causeAction = (GitHubPRCause) executableRun.getCause(GitHubPRCause.class);
                            if (nonNull(causeAction) && causeAction.getNumber() == number) {
                                LOGGER.info("Aborting {}", executableRun);
                                executor.interrupt(Result.ABORTED, new CauseOfInterruption() {
                                    @Override
                                    public String getShortDescription() {
                                        return "Newer PR will be scheduled.";
                                    }
                                });
                                aborted++;
                            }
                        }
                    }


//                    Queue.Task task = executor.getCurrentWorkUnit().work.getOwnerTask();
//                    if (task instanceof Job) {
//                        Job<?, ?> jobTask = (Job<?, ?>) task;
//
//                    }
                }
            }
        }

        return aborted;
    }

    /**
     * Cancel previous builds for specified PR id.
     */
    protected int cancelQueuedBuildByPrNumber(final int id) {
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

        return parameterizedJobMixIn.scheduleBuild2(quietPeriod, new CauseAction(cause), new ParametersAction(values),
                gitHubPRBadgeAction);
    }

    /**
     * @see jenkins.model.ParameterizedJobMixIn#getDefaultParametersValues()
     */
    protected List<ParameterValue> getDefaultParametersValues() {
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
