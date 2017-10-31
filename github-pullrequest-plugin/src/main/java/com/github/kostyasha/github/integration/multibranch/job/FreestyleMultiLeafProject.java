package com.github.kostyasha.github.integration.multibranch.job;

import com.github.kostyasha.github.integration.multibranch.job.leafs.MultiLeafProject;
import com.github.kostyasha.github.integration.multibranch.job.leafs.MultiLeafProjectDescriptor;
import hudson.Extension;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;

import javax.annotation.Nonnull;

public class FreestyleMultiLeafProject extends MultiLeafProject<FreeStyleProject, FreeStyleBuild> {
    public FreestyleMultiLeafProject(ItemGroup parent, String name) {
        super(parent, name);
    }


    @Extension
    public static class DescriptorImpl extends MultiLeafProjectDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return "FreeStyle MultiLeaf";
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new FreestyleMultiLeafProject(parent, name);
        }
    }

}
