package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Checks build result and allows run for publishers only for builds with specified result.
 *
 * @author Alina Karpovich
 */
public class StatusVerifier extends AbstractDescribableImpl<StatusVerifier> {

    private Result buildStatus;

    @DataBoundConstructor
    public StatusVerifier(Result buildStatus) {
        this.buildStatus = buildStatus;
    }

    public boolean isRunAllowed(Run<?, ?> run) {
        return run.getResult().isBetterOrEqualTo(buildStatus);
    }

    public Result getBuildStatus() {
        return buildStatus;
    }

    @Symbol("allowRunOnStatus")
    @Extension
    public static class DescriptorImpl extends Descriptor<StatusVerifier> {
        @Override
        public String getDisplayName() {
            return "Allow run only for specified status";
        }

        public ListBoxModel doFillBuildStatusItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Result.SUCCESS.toString());
            items.add(Result.UNSTABLE.toString());
            items.add(Result.FAILURE.toString());
            return items;
        }
    }
}
