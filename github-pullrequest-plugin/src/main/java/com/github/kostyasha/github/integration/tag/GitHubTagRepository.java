package com.github.kostyasha.github.integration.tag;

import com.github.kostyasha.github.integration.generic.GitHubRepository;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.isNull;

/**
 * Store local state of remote tags.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubTagRepository extends GitHubRepository<GitHubTagRepository> {

    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    private Map<String, GitHubTag> tags = new ConcurrentHashMap<>();

    /**
     * Object that represent GitHub repository to work with
     *
     * @param remoteRepository remote repository full name.
     */
    public GitHubTagRepository(GHRepository remoteRepository) throws IOException {
        super(remoteRepository);
    }

    public GitHubTagRepository(String repoFullName, URL url) {
        super(repoFullName, url);
    }

    @NonNull
    public Map<String, GitHubTag> getTags() {
        if (isNull(tags)) tags = new ConcurrentHashMap<>();
        return tags;
    }

    @Override
    public String getIconFileName() {
        return GitHubTag.getIconFileName();
    }

    @Override
    public String getDisplayName() {
        return "GitHub Tags";
    }

    @Override
    public String getUrlName() {
        return "github-tag";
    }

    @Override
    public void actualiseOnChange(@NonNull GHRepository ghRepository, @NonNull TaskListener listener) {
        if (changed) {
            listener.getLogger().println("Local settings changed, removing tags in repository state!");
            getTags().clear();
        }
    }

    @Override
    public FormValidation doClearRepo() throws IOException {
        return FormValidation.ok();
    }

    @Override
    public FormValidation doRunTrigger() throws IOException {
        return FormValidation.ok();
    }

    @Override
    public FormValidation doRebuildAllFailed() throws IOException {
        return FormValidation.ok();
    }

    @Override
    public FormValidation doBuild(StaplerRequest req) throws IOException {
        return FormValidation.ok();
    }

    @Override
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        return FormValidation.ok();
    }
}
