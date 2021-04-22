package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allows to change build result to specified value if there was publisher error.
 *
 * @author Alina Karpovich
 */
public class PublisherErrorHandler extends AbstractDescribableImpl<PublisherErrorHandler> {

    private Result buildStatus;

    @DataBoundConstructor
    public PublisherErrorHandler(Result buildStatus) {
        this.buildStatus = buildStatus;
    }

    public Result getBuildStatus() {
        return buildStatus;
    }

    public Result markBuildAfterError(Run<?, ?> run) {
        run.setResult(buildStatus);
        return buildStatus;
    }

    @Symbol("statusOnPublisherError")
    @Extension
    public static class DescriptorImpl extends Descriptor<PublisherErrorHandler> {
        @NonNull
        @Override
        public String getDisplayName() {
            return "Set build status if publisher failed";
        }

        public ListBoxModel doFillBuildStatusItems() {
            ListBoxModel items = new ListBoxModel();
            items.add(Result.UNSTABLE.toString());
            items.add(Result.FAILURE.toString());
            return items;
        }
    }
}
