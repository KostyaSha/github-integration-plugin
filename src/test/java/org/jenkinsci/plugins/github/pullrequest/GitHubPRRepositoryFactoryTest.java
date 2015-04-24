package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.coravy.hudson.plugins.github.GithubUrl;
import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.*;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
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
import java.util.Set;

import static org.mockito.Mockito.when;

/**
 * Created by Alina_Karpovich on 4/24/2015.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GithubProjectProperty.class, GithubUrl.class})
public class GitHubPRRepositoryFactoryTest {
    public static final String CONFIG_PATH = "src/test/resources";

    @Mock private AbstractProject<?, ?> job;
    @Mock private GitHubPRTrigger trigger;

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
        for (Iterator<GitHubPRRepository> iterator = (Iterator<GitHubPRRepository>) repoCollection.iterator(); iterator.hasNext();) {
            repo = iterator.next();
        }
        return repo;
    }

    /**
     * Requires @PrepareForTest({GithubProjectProperty.class, GithubUrl.class}) in class usig it.
     *
     * @param job mock job.
     * @param trigger mock trigger that is expected to be returned via job.getTrigger(GitHubPRTrigger.class).
     */
    public static void createForCommonExpectations(AbstractProject<?, ?> job, GitHubPRTrigger trigger) {
        createForCommonExpectations(CONFIG_PATH, job, trigger);
    }

    /**
     * Requires @PrepareForTest({GithubProjectProperty.class, GithubUrl.class}) in class usig it.
     *
     * @param filePath job's root directiry.
     * @param job mock job.
     * @param trigger mock trigger that is expected to be returned via job.getTrigger(GitHubPRTrigger.class).
     */
    public static void createForCommonExpectations(String filePath,
                                                    AbstractProject<?, ?> job,
                                                    GitHubPRTrigger trigger) {
        GithubUrl githubUrl = PowerMockito.mock(GithubUrl.class);
        GithubProjectProperty projectProperty = PowerMockito.mock(GithubProjectProperty.class);

        File file = new File(filePath);
        when(job.getRootDir()).thenReturn(file);
        when(job.getTrigger(GitHubPRTrigger.class)).thenReturn(trigger);
        when(job.getProperty(GithubProjectProperty.class)).thenReturn(projectProperty);
        when(projectProperty.getProjectUrl()).thenReturn(githubUrl);
    }
}
