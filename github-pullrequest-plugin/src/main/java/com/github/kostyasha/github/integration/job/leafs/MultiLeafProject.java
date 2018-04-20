package com.github.kostyasha.github.integration.job.leafs;

import com.cloudbees.hudson.plugins.folder.computed.ChildObserver;
import com.cloudbees.hudson.plugins.folder.computed.ComputedFolder;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;

import java.io.IOException;

/**
 * Leaf - is some variant of project kind i.e. tags, branch, etc.
 *
 * @param <P> - job type
 * @param <R> - Run type
 */
public class MultiLeafProject<P extends Job<P, R> & TopLevelItem,
        R extends Run<P, R>> extends ComputedFolder<P> {

    protected MultiLeafProject(ItemGroup parent, String name) {
        super(parent, name);
    }


    @Override
    protected void computeChildren(ChildObserver<P> observer, TaskListener listener)
            throws IOException, InterruptedException {
        listener.getLogger().println("Starting computation...");

    }

    @Override
    public MultiLeafProjectDescriptor getDescriptor() {
        return (MultiLeafProjectDescriptor) super.getDescriptor();
    }
}
