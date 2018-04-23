package com.github.kostyasha.github.integration.multibranch;

import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.head.GitHubBranchSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubPRSCMHead;
import com.github.kostyasha.github.integration.multibranch.head.GitHubTagSCMHead;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import jenkins.branch.Branch;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Clean up local repo cache when items get deleted
 */
@Extension
public class LocalRepoPlunger extends ItemListener {
    private static final Logger LOG = LoggerFactory.getLogger(LocalRepoPlunger.class);

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onDeleted(Item item) {
        if (!(item instanceof Job)) {
            return;
        }

        ItemGroup<? extends Item> parent = item.getParent();
        if (!(parent instanceof MultiBranchProject)) {
            return;
        }

        Job j = (Job) item;
        MultiBranchProject mb = (MultiBranchProject) parent;
        Branch branch = mb.getProjectFactory().getBranch(j);
        SCMHead head = branch.getHead();

        Consumer<GitHubRepo> plunger = null;
        if (head instanceof GitHubBranchSCMHead) {
            plunger = r -> r.getBranchRepository().getBranches().remove(head.getName());
        } else if (head instanceof GitHubTagSCMHead) {
            plunger = r -> r.getTagRepository().getTags().remove(head.getName());
        } else if (head instanceof GitHubPRSCMHead) {
            GitHubPRSCMHead prHead = (GitHubPRSCMHead) head;
            plunger = r -> r.getPrRepository().getPulls().remove(prHead.getPrNumber());
        }

        if (plunger != null) {
            for (SCMSource src : (List<SCMSource>) mb.getSCMSources()) {
                if (src instanceof GitHubSCMSource) {
                    GitHubSCMSource gsrc = (GitHubSCMSource) src;
                    plunger.accept(gsrc.getLocalRepo());
                    LOG.info("Plunging local data for {}", item.getFullName());
                }
            }
        }
    }
}
