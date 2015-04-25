package org.jenkinsci.plugins.github.pullrequest.extra;

import com.google.common.base.Splitter;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Queue;
import org.jenkinsci.plugins.blockqueuedjob.condition.BlockQueueCondition;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unblock when label found
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelUnblockQueueCondition extends BlockQueueCondition {
    private final static Logger LOGGER = Logger.getLogger(GitHubPRLabelUnblockQueueCondition.class.getName());

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
                    if (label.getLabelsSet().containsAll(causeLabels)) {
                        if (item.task instanceof AbstractProject<?, ?>) {
                            final AbstractProject<?, ?> abstractProject = (AbstractProject<?, ?>) item.task;
                            LOGGER.log(Level.FINE, "Unblocking job item {0} with matched labels {1}",
                                    new Object[]{ abstractProject.getFullName(), causeLabels.toString()});
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Extension(optional = true)
    public static class DescriptorImpl extends BlockQueueConditionDescriptor{

        @Override
        public String getDisplayName() {
            return "Unblock when GitHub PR label exists";
        }
    }
}
