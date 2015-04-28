package org.jenkinsci.plugins.github.pullrequest;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.coravy.hudson.plugins.github.GithubUrl;
import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.*;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.jenkinsci.plugins.github.pullrequest.util.TestUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.stubbing.OngoingStubbing;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
* Unit tests for GitHubPRRepository.
*/
@RunWith(PowerMockRunner.class)
@PrepareForTest({GithubProjectProperty.class, GithubUrl.class, BulkChange.class,
        Functions.class, Jenkins.class, User.class})
public class GitHubPRRepositoryTest {
    private static final int PR_REBUILD_ID = 1;

    //size of map that getAllPrBuilds() will return
    private static final int BUILD_MAP_SIZE = PR_REBUILD_ID + 2;

    @Mock private ItemGroup itemGroup;
    @Mock private AbstractProject<?, ?> job;
    @Mock private GitHubPRTrigger trigger;
    @Mock private RunList builds;
    @Mock private Iterator iterator;
    @Mock private AbstractBuild build;
    @Mock private GitHubPRCause cause;
    @Mock private StaplerRequest request;
    @Mock private User user;

    //mocked final classes
    @Mock private Jenkins instance;

    private GitHubPRRepositoryFactory factory = new GitHubPRRepositoryFactory();

    @Test
    public void getAllPrBuildsWithCause()  {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        Map<Integer, List<AbstractBuild<?, ?>>> prBuilds = repo.getAllPrBuilds();

        verify(iterator, times(BUILD_MAP_SIZE + 1)).hasNext();
        verify(iterator, times(BUILD_MAP_SIZE)).next();

        Assert.assertEquals(0, prBuilds.size());
    }

    @Test
    public void getAllPrBuildsNullCause()  {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        when(build.getCause(GitHubPRCause.class)).thenReturn(null);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        Map<Integer, List<AbstractBuild<?, ?>>> prBuilds = repo.getAllPrBuilds();

        Assert.assertEquals(0, prBuilds.size());
    }

    @Test
    public void doClearRepoPullsDeleted() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.DELETE, true);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));

        PowerMockito.mockStatic(BulkChange.class);
        when(BulkChange.contains(repo)).thenReturn(true);

        FormValidation formValidation = repo.doClearRepo();

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
        Assert.assertEquals(null, repo.getPulls());
    }

    @Test
    public void doClearRepoWithException() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.DELETE, true);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));

        PowerMockito.mockStatic(BulkChange.class);
        when(BulkChange.contains(repo)).thenThrow(new RuntimeException("bad save() for test"));

        FormValidation formValidation = repo.doClearRepo();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        Assert.assertEquals(null, repo.getPulls());
    }

    @Test
    public void doClearRepoForbidden() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.DELETE, false);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doClearRepo();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
        Assert.assertNotEquals(null, repo.getPulls());
    }

    @Test
    public void doRebuildFailedNoRebuildNeeded() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.BUILD, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildFailed();

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void doRebuildFailedWithRebuildPerformed() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.BUILD, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(build.getResult()).thenReturn(Result.FAILURE);
        when(build.getProject()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildFailed();

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void doRebuildFailedWithException() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.BUILD, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(build.getResult()).thenThrow(new RuntimeException("build.getResult() test exception"));

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildFailed();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    public void doRebuildFailedForbidden() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        hasPermissionExpectation(Item.BUILD, false);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildFailed();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    public void doRebuildWithRebuildPerformed() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        doRebuildCommonExpectations(true, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(build.getProject()).thenReturn(job);
        when(job.scheduleBuild(anyInt(), any(Cause.UserIdCause.class), Matchers.<Action>anyVararg()))
                .thenReturn(true);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
    }

    @Test
    public void doRebuildWarnNotScheduled() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        doRebuildCommonExpectations(false, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(build.getProject()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    @Test
    public void doRebuildWarnNotFound() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        doRebuildCommonExpectations(true, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(0);

        when(build.getProject()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
    }

    @Test
    public void doRebuildWithException() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        doRebuildCommonExpectations(true, true);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(build.getProject()).thenThrow(new RuntimeException("rebuild() test exception"));

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    @Test
    public void doRebuildForbidden() throws IOException {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger);
        doRebuildCommonExpectations(true, false);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
    }

    //to increase method coverage rate
    @Test
    public void checkGetters() {
        String fullName = "fullName";
        String url = "https://github.com/user/repo/";
        String prefix = "prefix";

        PowerMockito.mockStatic(Functions.class);
        when(Functions.getResourcePath()).thenReturn(prefix);

        GitHubPRRepository repo = new GitHubPRRepository(fullName, url,
                new HashMap<Integer, GitHubPRPullRequest>() );

        Assert.assertEquals(fullName, repo.getFullName());
        Assert.assertEquals(url, repo.getGithubUrl());
        Assert.assertEquals("github-pullrequest", repo.getUrlName());
        Assert.assertEquals(prefix + "/plugin/github-pullrequest/git-pull-request.svg", repo.getIconFileName());
    }

    private void doRebuildCommonExpectations(boolean hasParameter, boolean isAllowed) {
        hasPermissionExpectation(Item.BUILD, isAllowed);
        when(request.hasParameter(anyString())).thenReturn(hasParameter);
        if(hasParameter) {
            when(request.getParameter(anyString())).thenReturn(Integer.toString(PR_REBUILD_ID));
        }
    }

    private void hasPermissionExpectation(Permission permission, boolean isAllowed) {
        PowerMockito.mockStatic(Jenkins.class);
        when(Jenkins.getInstance()).thenReturn(instance);
        when(instance.hasPermission(permission)).thenReturn(isAllowed);
        PowerMockito.mockStatic(User.class);
        when(User.current()).thenReturn(user);
    }

    private void getAllPrBuildsCommonExpectations(int size) {
        when(job.getBuilds()).thenReturn(builds);
        when(builds.size()).thenReturn(size);
        when(job.getParent()).thenReturn(itemGroup);
        when(itemGroup.getFullName()).thenReturn("JobName");

        when(builds.iterator()).thenReturn(iterator);

        OngoingStubbing<Boolean> hasNextExpectation = size >= 1 ?
                when(iterator.hasNext()).thenReturn(true) : when(iterator.hasNext()).thenReturn(false) ;
        for (int i = 1; i < size; i++) {
            hasNextExpectation.thenReturn(true);
        }
        hasNextExpectation.thenReturn(false);

        OngoingStubbing<Object> nextExpectation = when(iterator.next()).thenReturn(build);
        for (int i = 1; i < size; i++) {
            nextExpectation.thenReturn(build);
        }
    }

    private void getAllPrBuildsNonNullCauseExpectations(int size) {
        when(build.getCause(GitHubPRCause.class)).thenReturn(cause);
        OngoingStubbing<Integer> expectation = null;
        for (int i = 0; i < size; i++) {
            if (expectation == null) {
                expectation = when(cause.getNumber()).thenReturn(i);
            } else {
                expectation.thenReturn(i);
            }
        }
    }
}
