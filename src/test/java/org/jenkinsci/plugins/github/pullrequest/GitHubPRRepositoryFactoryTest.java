package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.coravy.hudson.plugins.github.GithubUrl;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.ItemGroup;
import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Alina_Karpovich
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GithubProjectProperty.class, GithubUrl.class})
public class GitHubPRRepositoryFactoryTest {
    public static final String CONFIG_PATH = "src/test/resources";

    @Mock
    private ItemGroup parent;
    @Mock
    private AbstractProject<?, ?> job;
    @Mock
    private GitHubPRTrigger trigger;

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
        when(job.getTrigger(GitHubPRTrigger.class)).thenReturn(null);

        Collection<? extends Action> repoCollection = new GitHubPRRepositoryFactory().createFor(job);
        Assert.assertTrue(repoCollection instanceof List);
        Assert.assertTrue(repoCollection.isEmpty());
    }

    @Test
    public void shouldNotCreateRepoForTriggerWithExc() throws Exception {
        when(job.getTrigger(GitHubPRTrigger.class)).thenReturn(trigger);
        when(parent.getFullName()).thenReturn("mocked job");
        when(job.getParent()).thenReturn(parent);
        when(trigger.getRepoFullName(job)).thenThrow(Throwable.class);

        assertThat(new GitHubPRRepositoryFactory().createFor(job), hasSize(0));
    }

    private void createForCommonTest(String filePath) throws IOException, NoSuchFieldException, IllegalAccessException {
        createForCommonExpectations(filePath, job, trigger);

        GitHubPRRepository repo = getRepo(new GitHubPRRepositoryFactory().createFor(job));
        Field project = TestUtil.getPrivateField("project", GitHubPRRepository.class);
        Field configFileField = TestUtil.getPrivateField("configFile", GitHubPRRepository.class);
        XmlFile configFile = (XmlFile) configFileField.get(repo);

        Assert.assertEquals(job, project.get(repo));
        Assert.assertEquals(new File(filePath), configFile.getFile().getParentFile());
    }

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
    public static void createForCommonExpectations(Job<?, ?> job, GitHubPRTrigger trigger) {
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
                                                   Job<?, ?> job,
                                                   GitHubPRTrigger trigger) {
        GithubUrl githubUrl = PowerMockito.mock(GithubUrl.class);
        GithubProjectProperty projectProperty = PowerMockito.mock(GithubProjectProperty.class);

        File file = new File(filePath);
        when(job.getRootDir()).thenReturn(file);
        when(ghPRTriggerFromJob(job)).thenReturn(trigger);
        when(job.getProperty(GithubProjectProperty.class)).thenReturn(projectProperty);
        when(projectProperty.getProjectUrl()).thenReturn(githubUrl);
    }
}
