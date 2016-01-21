package org.jenkinsci.plugins.github.pullrequest.extra;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Unblock when label found
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelUnblockQueueCondition extends BlockQueueCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelUnblockQueueCondition.class);

    private GitHubPRLabel label;

    @DataBoundConstructor
    public GitHubPRLabelUnblockQueueCondition(GitHubPRLabel label) {
        this.label = label;
    }

    public GitHubPRLabel getLabel() {
        return label;
    }

    @Override
    public boolean isUnblocked(Queue.Item item) {
        final List<Cause> causes = item.getCauses();
        for (Cause cause : causes) {
            if (cause instanceof GitHubPRCause) {
                final GitHubPRCause gitHubPRCause = (GitHubPRCause) cause;
                final Set<String> causeLabels = gitHubPRCause.getLabels();
                if (getLabel() != null) {
                    if (causeLabels.containsAll(label.getLabelsSet())) {
                        if (item.task instanceof Job<?, ?>) {
                            final Job<?, ?> job = (Job<?, ?>) item.task;
                            LOGGER.debug("Unblocking job item {} with matched labels {}",
                                    job.getFullName(), label.getLabelsSet());
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BlockQueueConditionDescriptor {

        @Override
        public String getDisplayName() {
            return "Unblock when GitHub PR label exists";
        }
    }
}
