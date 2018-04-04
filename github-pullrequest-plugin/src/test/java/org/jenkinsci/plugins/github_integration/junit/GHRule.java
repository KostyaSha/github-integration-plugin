package org.jenkinsci.plugins.github_integration.junit;

import antlr.ANTLRException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCreatedEvent;
import hudson.util.Secret;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;
import static org.jenkinsci.plugins.github_integration.awaitility.GHBranchAppeared.ghBranchAppeared;
import static org.jenkinsci.plugins.github_integration.awaitility.GHFromServerConfigAppeared.ghAppeared;
import static org.jenkinsci.plugins.github_integration.awaitility.GHRepoAppeared.ghRepoAppeared;
import static org.jenkinsci.plugins.github_integration.awaitility.GHRepoDeleted.ghRepoDeleted;

/**
 * @author Kanstantsin Shautsou
 */
public class GHRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(GHRule.class);
    public static final String BRANCH1 = "branch-1";
    public static final String BRANCH2 = "branch-2";

    public static final String GH_TOKEN = System.getProperty("GH_TOKEN", System.getenv("GH_TOKEN"));

    private GHRepository ghRepo;
    private GitHub gitHub;
    private Git git;

    @CheckForNull
    private GitHubServerConfig gitHubServerConfig;

    @Nonnull
    private static JenkinsRule jRule;

    @Nonnull
    private static TemporaryFolder temporaryFolder;
    private File gitRootDir;

    public GHRule(JenkinsRule jRule, TemporaryFolder temporaryFolder) {
        this.jRule = jRule;
        this.temporaryFolder = temporaryFolder;
    }

    /**
     * github connection from configured github-plugin from global configuration.
     */
    public GitHub getGitHub() {
        return gitHub;
    }

    /**
     * GitHub repository that was created in {@link #before}.
     */
    public GHRepository getGhRepo() {
        return ghRepo;
    }


    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
                try {
                    base.evaluate();
                } finally {
                    after();
                }
            }
        };
    }

    private void after() {
        try {
            ghRepo.delete();
        } catch (IOException e) {
            LOG.error("Can't delete {}", ghRepo.getFullName(), e);
        }
    }


    public void before(Description description) throws IOException, GitAPIException, URISyntaxException, InterruptedException {
        String repoName = description.getClassName() + "-" + description.getMethodName();
        assertThat("Specify GH_TOKEN variable", GH_TOKEN, notNullValue());

        //reuse github client for GitHub preparation
        gitHubServerConfig = prepareGitHubPlugin();
        //FIXME no idea why github-plugin doesn't find configuration without delay, try delay
        await().timeout(20, SECONDS)
                .until(ghAppeared(gitHubServerConfig));

        gitHub = loginToGithub().apply(gitHubServerConfig);

        assertThat("Specify right GH_TOKEN variable!", gitHub, notNullValue());
        LOG.debug(gitHub.getRateLimit().toString());

        ghRepo = gitHub.getMyself().getRepository(repoName);
        if (ghRepo != null) {
            LOG.info("Deleting {}", ghRepo.getHtmlUrl());
            ghRepo.delete();
            await().pollInterval(3, SECONDS)
                    .timeout(120, SECONDS)
                    .until(ghRepoDeleted(gitHub, ghRepo.getFullName()));
        }

        ghRepo = gitHub.createRepository(repoName, "", "", true);
        LOG.info("Created {}", ghRepo.getHtmlUrl());

        await().pollInterval(2, SECONDS)
                .timeout(120, SECONDS)
                .until(ghRepoAppeared(gitHub, ghRepo.getFullName()));

        // prepare git
        gitRootDir = temporaryFolder.newFolder();

        git = Git.init().setDirectory(gitRootDir).call();

        writeStringToFile(new File(gitRootDir, "README.md"), "Test repo");
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage("Initial commit").call();

        final RefSpec refSpec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");

        final StoredConfig storedConfig = git.getRepository().getConfig();
        final RemoteConfig origin = new RemoteConfig(storedConfig, "origin");
        origin.addURI(new URIish(ghRepo.gitHttpTransportUrl()));
        origin.addPushRefSpec(refSpec);
        origin.update(storedConfig);
        storedConfig.save();

        commitFileToBranch(BRANCH1, BRANCH1 + ".file", "content", "commit for " + BRANCH1);

        commitFileToBranch(BRANCH2, BRANCH2 + ".file", "content", "commit for " + BRANCH2);

        git.checkout().setName("master").call();

        pushAll();
    }

    public void pushAll() throws GitAPIException {
        git.push()
                .setPushAll()
                .setProgressMonitor(new TextProgressMonitor())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(GH_TOKEN, ""))
                .call();
    }

    /**
     * Prepare global GitHub plugin configuration.
     * Nothing specific to job.
     */
    public static GitHubServerConfig prepareGitHubPlugin() {
        // prepare global jRule settings
        final StringCredentialsImpl cred = new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                null,
                "description",
                Secret.fromString(GH_TOKEN)
        );

        SystemCredentialsProvider.getInstance().getCredentials().add(cred);

        final GitHubPluginConfig gitHubPluginConfig = GitHubPlugin.configuration();

        final List<GitHubServerConfig> gitHubServerConfigs = new ArrayList<>();
        final GitHubServerConfig gitHubServerConfig = new GitHubServerConfig(cred.getId());
        gitHubServerConfig.setManageHooks(false);
        gitHubServerConfig.setClientCacheSize(0);
        gitHubServerConfigs.add(gitHubServerConfig);

        gitHubPluginConfig.setConfigs(gitHubServerConfigs);

        return gitHubServerConfig;
    }

    public static GitHubPRTrigger getPreconfiguredPRTrigger() throws ANTLRException {
        final ArrayList<GitHubPREvent> gitHubPREvents = new ArrayList<>();
        gitHubPREvents.add(new GitHubPROpenEvent());
        gitHubPREvents.add(new GitHubPRCommitEvent());

        final GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.CRON, gitHubPREvents);
        gitHubPRTrigger.setPreStatus(true);

        return gitHubPRTrigger;
    }

    public static GitHubBranchTrigger getDefaultBranchTrigger() throws ANTLRException {
        final ArrayList<GitHubBranchEvent> githubEvents = new ArrayList<>();
        githubEvents.add(new GitHubBranchCreatedEvent());

        final GitHubBranchTrigger githubTrigger = new GitHubBranchTrigger("", GitHubPRTriggerMode.CRON, githubEvents);
        githubTrigger.setPreStatus(true);

        return githubTrigger;
    }


    public static GithubProjectProperty getPreconfiguredProperty(GHRepository ghRepo) {
        return new GithubProjectProperty(ghRepo.getHtmlUrl().toString());
    }

    public void commitFileToBranch(String branch, String fileName, String content, String commitMessage)
            throws IOException, GitAPIException {
        final String beforeBranch = git.getRepository().getBranch();
        final List<Ref> refList = git.branchList().call();
        boolean exist = false;
        for (Ref ref : refList) {
            if (ref.getName().endsWith(branch)) {
                exist = true;
                break;
            }
        }
        if (!exist) {
            git.branchCreate().setName(branch).call();
        }

        git.checkout().setName(branch).call();

        writeStringToFile(new File(gitRootDir, fileName), content);
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage(commitMessage).call();
        git.push()
                .setPushAll()
                .setProgressMonitor(new TextProgressMonitor())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(GH_TOKEN, ""))
                .call();
        git.checkout().setName(beforeBranch).call();

        await().pollInterval(3, SECONDS)
                .timeout(120, SECONDS)
                .until(ghBranchAppeared(getGhRepo(), branch));
    }

//    because org.jenkinsci.plugins.github.internal.GitHubLoginFunction$OkHttpConnector is private
//    public static void flushCache(GitHub gitHub) {
//        try {
//            OkHttpConnector okHttpConnector = (OkHttpConnector) gitHub.getConnector();
//            Field field = okHttpConnector.getClass().getField("urlFactory");
//            field.setAccessible(true);
//            OkUrlFactory urlFactory = (OkUrlFactory) field.get(okHttpConnector);
//            urlFactory.client().getCache().flush();
//        } catch (Exception ex) {
//            Throwables.propagate(ex);
//        }
//        LOG.debug("Flushed GitHub connector cache");
//    }
}
