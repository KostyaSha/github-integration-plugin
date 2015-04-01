package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

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

    public Result markBuildAfterError(AbstractBuild<?, ?> build) {
        build.setResult(buildStatus);
        return buildStatus;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<PublisherErrorHandler> {
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
