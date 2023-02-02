package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.coravy.hudson.plugins.github.GithubUrl;
import hudson.XmlFile;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ItemGroup;
import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.utils.JobHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alina_Karpovich
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GithubProjectProperty.class, GithubUrl.class, JobHelper.class, AbstractItem.class,
        GitHubPRTrigger.DescriptorImpl.class})
public class GitHubPRRepositoryFactoryTest {
    public static final String CONFIG_PATH = "src/test/resources";

    @Mock
    private GHRepository ghRepository;

    @Mock
    private ItemGroup parent;
    @Mock
    private Job job;
    @Mock
    private GitHubPRTrigger trigger;

    @Mock
    private GitHubPRTrigger.DescriptorImpl descriptor;

    @Test
    public void createForConfigFileExists() throws IOException, NoSuchFieldException, IllegalAccessException {
        createForCommonTest(CONFIG_PATH);
    }

    @Test
    public void createForConfigFileIsAbsent() throws IOException, NoSuchFieldException, IllegalAccessException {
        createForCommonTest("some/path");
    }

    @Test
    public void createForConfigFileIsBad() throws IOException, NoSuchFieldException, IllegalAccessException {
        createForCommonTest(CONFIG_PATH + "/invalidConfig");
    }

    @Test
    public void createForNullTrigger() {
//        when(job.getTrigger(GitHubPRTrigger.class)).thenReturn(null);
        PowerMockito.mockStatic(JobHelper.class);
        given(ghPRTriggerFromJob(job))
                .willReturn(null);

        Collection<? extends Action> repoCollection = new GitHubPRRepositoryFactory().createFor(job);
        Assert.assertTrue(repoCollection instanceof List);
        Assert.assertTrue(repoCollection.isEmpty());
    }

    @Test
    public void shouldNotCreateRepoForTriggerWithExc() throws Exception {
//        when(job.getTrigger(GitHubPRTrigger.class)).thenReturn(trigger);
        PowerMockito.mockStatic(JobHelper.class);
        given(ghPRTriggerFromJob(job))
                .willReturn(trigger);

        when(parent.getFullName()).thenReturn("mocked job");
//        when(job.getParent()).thenReturn(parent);
        when(trigger.getRepoFullName(job)).thenThrow(new RuntimeException());

        assertThat(new GitHubPRRepositoryFactory().createFor(job), hasSize(0));
    }

    private void createForCommonTest(String filePath) throws IOException, NoSuchFieldException, IllegalAccessException {
        PowerMockito.mockStatic(GitHubPRTrigger.DescriptorImpl.class);
        given(GitHubPRTrigger.DescriptorImpl.get())
                .willReturn(descriptor);
        when(descriptor.isActualiseOnFactory()).thenReturn(false);

        createForCommonExpectations(filePath, job, trigger);

        when(trigger.getRemoteRepository()).thenReturn(ghRepository);

        GitHubPRRepository repo = getRepo(new GitHubPRRepositoryFactory().createFor(job));
        assertThat(repo, notNullValue());
        XmlFile configFile = repo.getConfigFile();

        Assert.assertEquals(job, trigger.getJob());
        Assert.assertEquals(new File(filePath), configFile.getFile().getParentFile());
    }

    @CheckForNull
    public static GitHubPRRepository getRepo(Collection<? extends Action> repoCollection) {
        GitHubPRRepository repo = null;
        for (Iterator<GitHubPRRepository> iterator = (Iterator<GitHubPRRepository>) repoCollection.iterator(); iterator.hasNext(); ) {
            repo = iterator.next();
        }
        return repo;
    }

    /**
     * Requires @PrepareForTest({GithubProjectProperty.class, GithubUrl.class}) in class usig it.
     *
     * @param job     mock job.
     * @param trigger mock trigger that is expected to be returned via job.getTrigger(GitHubPRTrigger.class).
     */
    public static void createForCommonExpectations(AbstractProject<?, ?> job, GitHubPRTrigger trigger) {
        createForCommonExpectations(CONFIG_PATH, job, trigger);
    }

    /**
     * Requires @PrepareForTest({GithubProjectProperty.class, GithubUrl.class}) in class usig it.
     *
     * @param filePath job's root directory.
     * @param job      mock job.
     * @param trigger  mock trigger that is expected to be returned via job.getTrigger(GitHubPRTrigger.class).
     */
    public static void createForCommonExpectations(String filePath,
                                                   Job job,
                                                   GitHubPRTrigger trigger) {
        GithubUrl githubUrl = PowerMockito.mock(GithubUrl.class);
        when(githubUrl.toString()).thenReturn("http://blaur");
        GithubProjectProperty projectProperty = PowerMockito.mock(GithubProjectProperty.class);

        File file = new File(filePath);
        when(job.getRootDir()).thenReturn(file);
        when(job.getFullName()).thenReturn("jobFullName");

        PowerMockito.mockStatic(JobHelper.class);
        given(ghPRTriggerFromJob(job))
                .willReturn(trigger);
        when(trigger.getJob()).thenReturn(job);

        when(trigger.getRepoFullName(job)).thenReturn(mock(GitHubRepositoryName.class));
        when(job.getProperty(GithubProjectProperty.class)).thenReturn(projectProperty);
        when(projectProperty.getProjectUrl()).thenReturn(githubUrl);
    }
}
