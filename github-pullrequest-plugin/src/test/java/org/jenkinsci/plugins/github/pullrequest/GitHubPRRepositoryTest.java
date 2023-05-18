package org.jenkinsci.plugins.github.pullrequest;

import hudson.BulkChange;
import hudson.Functions;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.utils.JobHelper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GitHubPRRepository.
 */
@Ignore(value = "Mock issues")
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRRepositoryTest {
    private static final int PR_REBUILD_ID = 1;

    //size of map that getAllPrBuilds() will return
    private static final int BUILD_MAP_SIZE = PR_REBUILD_ID + 2;

    @Mock
    private ItemGroup<Item> itemGroup;
    @Mock
    private Job job;
    @Mock
    private GitHubPRTrigger trigger;
    @Mock
    private RunList builds;
    @Mock
    private Iterator iterator;
    @Mock
    private Run<?, ?> run;
    @Mock
    private GitHubPRCause cause;
    @Mock
    private StaplerRequest request;
    @Mock
    private User user;
    @Mock
    private GHRepository ghRepository;

    //mocked final classes
    @Mock
    private Jenkins instance;

    private GitHubPRRepositoryFactory factory = new GitHubPRRepositoryFactory();

    @Test
    public void getAllPrBuildsWithCause() {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        Map<Integer, List<Run<?, ?>>> prBuilds = repo.getAllPrBuilds();

        verify(iterator, times(BUILD_MAP_SIZE + 1)).hasNext();
        verify(iterator, times(BUILD_MAP_SIZE)).next();

        Assert.assertEquals(0, prBuilds.size());
      }
    }

    @Test
    public void getAllPrBuildsNullCause() {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        when(run.getCause(GitHubPRCause.class)).thenReturn(null);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        Map<Integer, List<Run<?, ?>>> prBuilds = repo.getAllPrBuilds();

        Assert.assertEquals(0, prBuilds.size());
      }
    }

    @Test
    public void doClearRepoPullsDeleted() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class);
           MockedStatic<BulkChange> staticBulkChange = mockStatic(BulkChange.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.DELETE, true, mockedJenkins, mockedUser);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));

        staticBulkChange.when(() -> BulkChange.contains(repo)).thenReturn(true);

        assertThat(repo.doClearRepo().kind, equalTo(FormValidation.Kind.OK));
        assertThat(repo.getPulls().keySet(), hasSize(0));
      }
    }

    @Test
    public void doClearRepoWithException() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class);
           MockedStatic<BulkChange> staticBulkChange = mockStatic(BulkChange.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.DELETE, true, mockedJenkins, mockedUser);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));

        staticBulkChange.when(() -> BulkChange.contains(repo)).thenThrow(new RuntimeException("bad save() for test"));

        assertThat(repo.doClearRepo().kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(repo.getPulls().keySet(), hasSize(0));
      }
    }

    @Test
    public void doClearRepoForbidden() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.DELETE, false, mockedJenkins, mockedUser);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));

        assertThat(repo.doClearRepo().kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(repo.getPulls().keySet(), hasSize(greaterThan(0)));
      }
    }

    @Test
    public void doRebuildFailedNoRebuildNeeded() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.BUILD, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildAllFailed();

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
      }
    }

    @Test
    public void doRebuildFailedWithRebuildPerformed() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.BUILD, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(run.getResult()).thenReturn(Result.FAILURE);
        when(run.getParent()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        assertThat(repo, notNullValue());
        FormValidation formValidation = repo.doRebuildAllFailed();

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
      }
    }

    @Test
    public void doRebuildFailedWithException() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.BUILD, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(run.getResult()).thenThrow(new RuntimeException("run.getResult() test exception"));

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildAllFailed();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
      }
    }

    @Test
    public void doRebuildFailedForbidden() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        hasPermissionExpectation(Item.BUILD, false, mockedJenkins, mockedUser);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuildAllFailed();

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
      }
    }

    // FIXME: 1/7/16
    @Ignore
    @Test
    public void doRebuildWithRebuildPerformed() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        doRebuildCommonExpectations(true, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(run.getParent()).thenReturn(job);
//        when(job.scheduleBuild(anyInt(), any(Cause.UserIdCause.class), Matchers.<Action>anyVararg()))
//                .thenReturn(true);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.OK, formValidation.kind);
      }
    }

    @Test
    public void doRebuildWarnNotScheduled() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        doRebuildCommonExpectations(false, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(run.getParent()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
      }
    }

    @Test
    public void doRebuildWarnNotFound() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        doRebuildCommonExpectations(true, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(0);

        when(run.getParent()).thenReturn(job);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.WARNING, formValidation.kind);
      }
    }

    @Test
    public void doRebuildWithException() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        doRebuildCommonExpectations(true, true, mockedJenkins, mockedUser);
        getAllPrBuildsCommonExpectations(BUILD_MAP_SIZE);
        getAllPrBuildsNonNullCauseExpectations(BUILD_MAP_SIZE);

        when(run.getParent()).thenThrow(new RuntimeException("rebuild() test exception"));

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
      }
    }

    @Test
    public void doRebuildForbidden() throws IOException {
      try (MockedStatic<JobHelper> staticJobHelper = mockStatic(JobHelper.class);
           MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class);
           MockedStatic<User> mockedUser = mockStatic(User.class)) {
        GitHubPRRepositoryFactoryTest.createForCommonExpectations(job, trigger, staticJobHelper);
        doRebuildCommonExpectations(true, false, mockedJenkins, mockedUser);

        GitHubPRRepository repo = GitHubPRRepositoryFactoryTest.getRepo(factory.createFor(job));
        FormValidation formValidation = repo.doRebuild(request);

        Assert.assertEquals(FormValidation.Kind.ERROR, formValidation.kind);
      }
    }

    //to increase method coverage rate
    @Test
    public void checkGetters() throws IOException {
      try (MockedStatic<Functions> mockedFunctions = mockStatic(Functions.class)) {
        String fullName = "fullName";
        URL url = new URL("https://github.com/user/repo/");
        String prefix = "prefix";

        mockedFunctions.when(Functions::getResourcePath).thenReturn(prefix);
        when(ghRepository.getFullName()).thenReturn("user/repo");
        when(ghRepository.getHtmlUrl()).thenReturn(url);

        GitHubPRRepository repo = new GitHubPRRepository(ghRepository);

        Assert.assertEquals(fullName, repo.getFullName());
        Assert.assertEquals(url, repo.getGithubUrl());
        Assert.assertEquals("github-pullrequest", repo.getUrlName());
        Assert.assertEquals(prefix + "/plugin/github-pullrequest/git-pull-request.svg", repo.getIconFileName());
      }
    }

    private void doRebuildCommonExpectations(boolean hasParameter, boolean isAllowed, MockedStatic<Jenkins> mockedJenkins, MockedStatic<User> mockedUser) {
        hasPermissionExpectation(Item.BUILD, isAllowed, mockedJenkins, mockedUser);
        when(request.hasParameter(anyString())).thenReturn(hasParameter);
        if (hasParameter) {
            when(request.getParameter(anyString())).thenReturn(Integer.toString(PR_REBUILD_ID));
        }
    }

    private void hasPermissionExpectation(Permission permission, boolean isAllowed, MockedStatic<Jenkins> mockedJenkins, MockedStatic<User> mockedUser) {
        mockedJenkins.when(Jenkins::getInstance).thenReturn(instance);
        when(instance.hasPermission(permission)).thenReturn(isAllowed);
        mockedUser.when(User::current).thenReturn(user);
    }

    private void getAllPrBuildsCommonExpectations(int size) {
        when(job.getBuilds()).thenReturn(builds);
        when(builds.size()).thenReturn(size);
        when(job.getParent()).thenReturn(itemGroup);
        when(itemGroup.getFullName()).thenReturn("JobName");

        when(builds.iterator()).thenReturn(iterator);

        OngoingStubbing<Boolean> hasNextExpectation = size >= 1 ?
                when(iterator.hasNext()).thenReturn(true) : when(iterator.hasNext()).thenReturn(false);
        for (int i = 1; i < size; i++) {
            hasNextExpectation.thenReturn(true);
        }
        hasNextExpectation.thenReturn(false);

        OngoingStubbing<Object> nextExpectation = when(iterator.next()).thenReturn(run);
        for (int i = 1; i < size; i++) {
            nextExpectation.thenReturn(run);
        }
    }

    private void getAllPrBuildsNonNullCauseExpectations(int size) {
        when(run.getCause(GitHubPRCause.class)).thenReturn(cause);
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
