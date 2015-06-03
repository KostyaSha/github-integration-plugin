package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.queue.QueueTaskFuture;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.kohsuke.github.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger.DescriptorImpl.getJenkinsInstance;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.*;

/**
 * GitHub Pull Request trigger.
 * Planned trigger modes:
 * - just incoming hooks, without persist (save PR state to local xml)
 * - hooks with persist
 * - cron run, persist
 * <p>
 * Restrictions can't have resolver, so they separate and provide security check methods:
 * - Target branch restriction {@link org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction}
 * - User restriction (check comments, labels, etc) {@link org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction}
 * (whitelist manipulations using comments is also allowed)
 * <p>
 * Event triggering is modular. Now they can be split to any events:
 * - Trigger by comment
 * - Trigger when PR opened
 * - Trigger when PR closed
 * - Trigger by label
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRTrigger extends Trigger<AbstractProject<?, ?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRTrigger.class);
    private static final Cause NO_CAUSE = null;

    //TODO replace with {@link GitHubRepositoryName.class} ?
    private static final Pattern GH_FULL_REPO_NAME = Pattern.compile("^(http[s]?://[^/]*)/([^/]*/[^/]*).*");

    @CheckForNull
    private GitHubPRTriggerMode triggerMode = GitHubPRTriggerMode.CRON;
    @CheckForNull
    private final List<GitHubPREvent> events;
    /**
     * Set PR(commit) status before build. No configurable message for it.
     */
    private boolean preStatus = false;
    private boolean cancelQueued = false;
    private boolean skipFirstRun = false;
    @CheckForNull
    private GitHubPRUserRestriction userRestriction;
    @CheckForNull
    private GitHubPRBranchRestriction branchRestriction;

    // for performance
    private transient String repoFullName;
    private transient GHRepository remoteRepository;

    @CheckForNull
    private transient GitHubPRPollingLogAction pollingLogAction;

    @DataBoundConstructor
    public GitHubPRTrigger(String spec,
                           GitHubPRTriggerMode triggerMode,
                           List<GitHubPREvent> events) throws ANTLRException {
        super(spec);
        this.triggerMode = triggerMode;
        this.events = Util.fixNull(events);
    }

    @DataBoundSetter
    public void setPreStatus(boolean preStatus) {
        this.preStatus = preStatus;
    }

    @DataBoundSetter
    public void setCancelQueued(boolean cancelQueued) {
        this.cancelQueued = cancelQueued;
    }

    @DataBoundSetter
    public void setSkipFirstRun(boolean skipFirstRun) {
        this.skipFirstRun = skipFirstRun;
    }

    @DataBoundSetter
    public void setUserRestriction(GitHubPRUserRestriction userRestriction) {
        this.userRestriction = userRestriction;
    }

    @DataBoundSetter
    public void setBranchRestriction(GitHubPRBranchRestriction branchRestriction) {
        this.branchRestriction = branchRestriction;
    }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        LOGGER.info("Starting GitHub Pull Request trigger for project {}", project.getName());
        super.start(project, newInstance);

        if (getTriggerMode() != GitHubPRTriggerMode.CRON) {
            //TODO implement
            return;
        }

    }

    // there race conditions when job is null but trigger exists
    public String getRepoFullName() {
        return getRepoFullName(job);
    }

    public String getRepoFullName(AbstractProject<?, ?> job) {
        if (repoFullName != null && !repoFullName.trim().equals("")) {
            return repoFullName;
        }

        if (job == null) {
            LOGGER.error("job object is null, race condition?");
            throw new IllegalStateException("Job object is null");
        }

        if (job.getProperty(GithubProjectProperty.class) == null) {
            LOGGER.info("GitHub project not set up, cannot start GitHub PR trigger for job {}", job);
            throw new IllegalArgumentException("GitHub project property is not defined. " +
                    "Cannot start GitHub PR trigger for job " + job.getName());
        }

        final GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);
        if (ghpp == null || ghpp.getProjectUrl() == null) {
            throw new IllegalArgumentException("A GitHub project url is required.");
        }

        String baseUrl = ghpp.getProjectUrl().baseUrl();
        Matcher m = GH_FULL_REPO_NAME.matcher(baseUrl);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid GitHub project url: %s", baseUrl));
        }

        repoFullName = m.group(2);
        return repoFullName;
    }

    @Override
    public void run() {
        if (getTriggerMode() != null && getTriggerMode() == GitHubPRTriggerMode.CRON) {
            doRun();
        }
    }

    /**
     * For running from external places. Goes to queue.
     */
    public void queueRun(AbstractProject<?, ?> job) {
        this.job = job;
        getDescriptor().queue.execute(new Runnable() {
            @Override
            public void run() {
                doRun();
            }
        });
    }

    public void doRun() {
        if (job == null || job.isDisabled()) {
            LOGGER.debug("Job {} is disabled, but trigger run!", job == null ? "no job" : job.getFullName());
            return;
        }

        if (!((getTriggerMode() == GitHubPRTriggerMode.CRON) ||
                (getTriggerMode() == GitHubPRTriggerMode.HEAVY_HOOKS))) {
            return;
        }

        long startTime = System.currentTimeMillis();

        List<GitHubPRCause> causes = Collections.emptyList();

        try (StreamTaskListener listener = new StreamTaskListener(getPollingLogAction().getLogFile())) {
            final PrintStream logger = listener.getLogger();
            logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
            LOGGER.debug("Running GitHub Pull Request trigger check.");

            GitHubPRRepository localRepository = null;
            try {
                localRepository = job.getAction(GitHubPRRepository.class);
                causes = check(localRepository, listener);
            } catch (IOException e) {
                listener.error("Can't save repository state, because " + e.getMessage());
                LOGGER.error("Can't save repository state, because: '{}'", e.getMessage());
            }

            if (localRepository != null) {
                try {
                    localRepository.save();
                } catch (IOException e) {
                    listener.error("Can't save repository state, because " + e.getMessage());
                    LOGGER.error("Can't save repository state, because: '{}'", e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            LOGGER.info("End  GitHub Pull Request trigger check. Summary time: {}ms", duration);
            logger.println("Finished at " + DateFormat.getDateTimeInstance().format(new Date())
                    + ", duration " + duration + "ms");
        } catch (Throwable e) {
            LOGGER.error("can't trigger build {}", e.getMessage());
            return;
        }

        for (GitHubPRCause cause : causes) {
            try {
                cause.setPollingLog(pollingLogAction.getLogFile());
                build(cause);
            } catch (IOException e) {
                LOGGER.error("can't trigger build {}", e.getMessage());
            }
        }
    }

    @CheckForNull
    public GitHubPRPollingLogAction getPollingLogAction() {
        if (pollingLogAction == null && job != null) {
            pollingLogAction = new GitHubPRPollingLogAction(job);
        }

        return pollingLogAction;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (getPollingLogAction() == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(getPollingLogAction());
    }

    /**
     * replace with {@link com.cloudbees.jenkins.GitHubRepositoryName} ?
     */

    /**
     * runs check of local (last) Repository state (list of PRs) vs current remote state
     * - local state store only last open PRs
     * - if last open PR <-> now closed -> should trigger only when ClosePREvent exist
     * - last open PR <-> now changed -> trigger only
     * - special comment in PR -> trigger
     */
    public List<GitHubPRCause> check(GitHubPRRepository localRepository, TaskListener listener) throws IOException {
        final PrintStream logger = listener.getLogger();

        GHRateLimit rateLimitBefore = getGitHub().getRateLimit();
        LOGGER.debug("GitHub rate limit before check: {}", rateLimitBefore);
        logger.println("GitHub rate limit before check: " + rateLimitBefore);
        int checkedPR = 0;

        final ArrayList<GitHubPRCause> gitHubPRCauses = new ArrayList<GitHubPRCause>();

        // get local and remote list of PRs
        Map<Integer, GitHubPRPullRequest> localPulls = localRepository.getPulls();
        String repoFullName1 = getRepoFullName();
        GHRepository ghRepository = getGitHub().getRepository(repoFullName1);

        List<GHPullRequest> remotePulls;
        remotePulls = ghRepository.getPullRequests(GHIssueState.OPEN);
        // add PRs that was closed on remote
        for (Map.Entry<Integer, GitHubPRPullRequest> localPr : localPulls.entrySet()) {
            boolean contains = false;

            for (GHPullRequest remotePR : remotePulls) {
                if (remotePR.getNumber() == localPr.getKey()) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                remotePulls.add(ghRepository.getPullRequest(localPr.getKey()));
            }
        }

        for (GHPullRequest remotePR : remotePulls) {
            checkedPR++;

            //null if local not existed before
            @CheckForNull GitHubPRPullRequest localPR = localPulls.get(remotePR.getNumber());

            if (!isUpdated(remotePR, localPR)) { // light check
                LOGGER.debug("PR #{} '{}' not changed", remotePR.getNumber(), remotePR.getTitle());
                logger.println("PR #" + remotePR.getNumber() + " '" + remotePR.getTitle() + "' not changed");
                continue;
            }

            switch (remotePR.getState()) {
                case OPEN:
                    localPulls.put(remotePR.getNumber(), new GitHubPRPullRequest(remotePR));
                    break;
                case CLOSED:
                    localPulls.remove(remotePR.getNumber()); // don't store
                    break;
            }

            if (userRestriction != null) {
                userRestriction.populate(remotePR, localPR, this);
            }

            if (skipFirstRun) {
                LOGGER.info("Skipping first run for {} and PR #{}",
                        job.getFullName(), remotePR.getNumber());
                logger.println("Skipping first run for " + job.getFullName() + " and PR #" + remotePR.getNumber());
                continue;
            }

            if (branchRestriction != null && branchRestriction.isBranchBuildAllowed(remotePR)) {
                LOGGER.warn("Skipping #{} {} because of branch restriction",
                        remotePR.getNumber(), remotePR.getTitle());
                logger.println("Skipping #" + remotePR.getNumber() + " " + remotePR.getTitle() + " because of branch restriction");
                continue;
            }

            if (userRestriction != null && !userRestriction.isWhitelisted(remotePR.getUser())) {
                LOGGER.warn("Skipping #{} {} because of user restriction (user - {})",
                        remotePR.getNumber(), remotePR.getTitle(), remotePR.getUser());
                logger.println("Skipping #" + remotePR.getNumber() + " " + remotePR.getTitle()
                        + " because of user restriction (user - " + remotePR.getUser() + ")");
                continue;
            }

            for (GitHubPREvent event : getEvents()) {  // waterfall, first matched win
                try {
                    GitHubPRCause cause = event.check(this, remotePR, localPR, listener);
                    if (cause != null) {
                        if (cause.isSkip()) {
                            LOGGER.debug("Skipping PR #{}", remotePR.getNumber());
                            logger.println("Skipping PR #" + remotePR.getNumber());
                            break;
                        } else {
                            LOGGER.debug("Triggering build for PR #'{}', because {}",
                                    remotePR.getNumber(), cause.getReason());
                            logger.println("Triggering build for PR #" + remotePR.getNumber() + " because " + cause.getReason());
                            gitHubPRCauses.add(cause);
                            // don't check other events
                            break;
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("Can't check trigger event", e);
                    listener.error("Skip event failed, so skipping PR");
                    break;
                }

            }
        }

        if (skipFirstRun) {
            LOGGER.info("Skipping first run for {}", job.getFullName());
            skipFirstRun = false;
            trySave(); //TODO or better fail with IOException?
        }

        GHRateLimit rateLimitAfter = getGitHub().getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOGGER.info("GitHub rate limit after check: {}, consumed: {}, checked PRs: {}",
                rateLimitAfter, consumed, checkedPR);
        return gitHubPRCauses;
    }

    /**
     * lightweight check that comments and time were changed
     */
    public boolean isUpdated(GHPullRequest pr, GitHubPRPullRequest localPR) throws IOException {
        if (localPR == null) {
            return true; // we don't know yet
        }

        boolean prUpd = localPR.getPrUpdatedAt().compareTo(pr.getUpdatedAt()) < 0; // by time
        boolean issueUpd = localPR.getIssueUpdatedAt().compareTo(pr.getIssueUpdatedAt()) < 0;
        boolean headUpd = !localPR.getHeadSha().equals(pr.getHead().getSha()); // or head?
        boolean updated = prUpd || issueUpd || headUpd;

        if (updated) {
            LOGGER.info("Pull request #{} was updated at: {} by {}",
                    localPR.getNumber(), localPR.getPrUpdatedAt(), localPR.getUserLogin());
        }

        return updated;
    }

    public void build(@Nonnull GitHubPRCause cause) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Run queued");

        if (cancelQueued && cancelQueuedBuildByPrNumber(cause.getNumber())) {
            sb.append("Queued builds aborted.");
        }

        QueueTaskFuture<?> queueTaskFuture = startJob(cause);
        if (queueTaskFuture == null) {
            LOGGER.error("Job didn't start");
        }

        LOGGER.info(sb.toString());

        GitHub connection = getGitHub();    // remote connection
        if (connection != null && preStatus) {
            GHRepository repository = connection.getRepository(repoFullName);
            repository.createCommitStatus(cause.getHeadSha(),
                    GHCommitState.PENDING,
                    null,
                    sb.toString(),
                    job.getFullName());
        }

    }

    /**
     * Cancel previous builds for specified PR id.
     */
    private boolean cancelQueuedBuildByPrNumber(int id) {
        Queue queue = getJenkinsInstance().getQueue();
        List<Queue.Item> approximateItemsQuickly = queue.getApproximateItemsQuickly();

        for (Queue.Item item : approximateItemsQuickly) {
            List<? extends Action> allActions = item.getAllActions();
            for (Action action : allActions) {
                if (action instanceof CauseAction) {
                    CauseAction causeAction = (CauseAction) action;
                    for (Cause cause : causeAction.getCauses()) {
                        if (cause instanceof GitHubPRCause) {
                            GitHubPRCause gitHubPRCause = (GitHubPRCause) cause;
                            if (gitHubPRCause.getNumber() == id) {
                                queue.cancel(item);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void stop() {
        //TODO clean hooks?
        if (job != null) {
            LOGGER.info("Stopping the GitHub PR trigger for project {}", job.getFullName());
        }
        super.stop();
    }

    private QueueTaskFuture<?> startJob(GitHubPRCause cause) {
        List<ParameterValue> values = getDefaultParametersValues();
        values.addAll(asList(
                TRIGGER_SENDER_AUTHOR.param(cause.getTriggerSenderName()),
                TRIGGER_SENDER_EMAIL.param(cause.getTriggerSenderEmail()),
                COMMIT_AUTHOR_NAME.param(cause.getCommitAuthorName()),
                COMMIT_AUTHOR_EMAIL.param(cause.getCommitAuthorEmail()),
                TARGET_BRANCH.param(cause.getTargetBranch()),
                SOURCE_BRANCH.param(cause.getSourceBranch()),
                AUTHOR_EMAIL.param(cause.getPRAuthorEmail()),
                SHORT_DESC.param(cause.getShortDescription()),
                TITLE.param(cause.getTitle()),
                URL.param(cause.getHtmlUrl().toString()),
                SOURCE_REPO_OWNER.param(cause.getSourceRepoOwner()),
                HEAD_SHA.param(cause.getHeadSha()),
                COND_REF.param(cause.getCondRef()),
                CAUSE_SKIP.param(cause.isSkip()),
                NUMBER.param(String.valueOf(cause.getNumber()))
        ));

        return this.job.scheduleBuild2(job.getQuietPeriod(), NO_CAUSE,
                asList(new CauseAction(cause), new ParametersAction(values)));
    }

//    /**
//     * Find the previous BuildData for the given pull request number; this may return null
//     */
//    private @CheckForNull BuildData findPreviousBuildByPRNumber(StringParameterValue prNumber) {
//        // find the previous build for this particular pull request, it may not be the last build
//        for (Run<?, ?> r : job.getBuilds()) {
//            ParametersAction pa = r.getAction(ParametersAction.class);
//            if (pa != null) {
//                for (ParameterValue pv : pa.getParameters()) {
//                    if (pv.equals(prNumber)) {
//                        return r.getAction(BuildData.class);
//                    }
//                }
//            }
//        }
//        return null;
//    }

    /**
     * @see jenkins.model.ParameterizedJobMixIn#getDefaultParametersValues()
     */
    private List<ParameterValue> getDefaultParametersValues() {
        ParametersDefinitionProperty paramDefProp = job.getProperty(ParametersDefinitionProperty.class);
        List<ParameterValue> defValues = new ArrayList<>();

        /*
         * This check is made ONLY if someone will call this method even if isParametrized() is false.
         */
        if (paramDefProp == null) {
            return defValues;
        }

        /* Scan for all parameter with an associated default values */
        for (ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

            if (defaultValue != null) {
                defValues.add(defaultValue);
            }
        }

        return defValues;
    }


    public boolean isPreStatus() {
        return preStatus;
    }

    public boolean isCancelQueued() {
        return cancelQueued;
    }

    public boolean isSkipFirstRun() {
        return skipFirstRun;
    }

    public GitHubPRTriggerMode getTriggerMode() {
        return triggerMode;
    }

    public List<GitHubPREvent> getEvents() {
        return events;
    }

    /**
     * @deprecated introduced transient factory that should provide, but let's wrap here call ??
     */
    public GitHubPRRepository getLocalRepository(String repoFullName) throws IOException {
        return job.getAction(GitHubPRRepository.class);
    }

    public GHRepository getRemoteRepo() throws IOException {
        if (remoteRepository == null) {
            remoteRepository = getGitHub().getRepository(getRepoFullName());
        }
        return remoteRepository;
    }

    public GitHub getGitHub() throws IOException {
        GitHub gh;
        try {
            gh = getDescriptor().getGitHub();
        } catch (FileNotFoundException ex) {
            LOGGER.info("Can't connect to GitHub {}. Bad Global plugin configuration.", ex.getMessage());
            throw new IOException("Can't connect to GitHub: " + ex.getMessage() + ". Bad Global plugin configuration.", ex);
        } catch (IOException ex) {
            LOGGER.error("Can't connect to GitHub {}. Bad Global plugin configuration.", ex.getMessage());
            throw ex;
        } catch (Throwable t) {
            LOGGER.debug("Can't connect to GitHub {}. Bad Global plugin configuration.", t.getMessage());
            throw new IOException("Can't connect to GitHub: " + t.getMessage() + ". Bad Global plugin configuration.", t);
        }

        if (gh == null) {
            throw new IOException("Can't connect to GitHub");
        }

        return gh;
    }

    public void trySave() {
        try {
            job.save();
        } catch (IOException e) {
            LOGGER.error("Error while saving job to file", e);
        }
    }

    public GitHubPRUserRestriction getUserRestriction() {
        return userRestriction;
    }

    public GitHubPRBranchRestriction getBranchRestriction() {
        return branchRestriction;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private static final Logger LOGGER = LoggerFactory.getLogger(DescriptorImpl.class);

        private final transient SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        private String apiUrl = "https://api.github.com";
        private String whitelistUserMsg = ".*add\\W+to\\W+whitelist.*";
        private String spec = "H/5 * * * *";

        private String username;
        private String password;
        private String accessToken;
        private String publishedURL;

        private transient GitHub gh;
        private int cacheSize = 20; // MB

        private transient int oldHash = 0;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Build GitHub pull requests";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
//            req.bindJSON(this, formData);
            apiUrl = formData.getString("apiUrl");
            username = formData.getString("username");
            password = formData.getString("password");
            accessToken = formData.getString("accessToken");
            publishedURL = formData.getString("publishedURL");
            whitelistUserMsg = formData.getString("whitelistUserMsg");
            spec = formData.getString("spec");
            cacheSize = formData.getInt("cacheSize");

            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckServerAPIUrl(@QueryParameter String value) {
            if ("https://api.github.com".equals(value)) {
                return FormValidation.ok();
            }
            if (value.endsWith("/api/v3") || value.endsWith("/api/v3/")) {
                return FormValidation.ok();
            }
            return FormValidation.warning("GitHub API URI is \"https://api.github.com\". GitHub Enterprise API URL ends with \"/api/v3\"");
        }

        // create token for specified login/password
        public FormValidation doCreateApiToken(@QueryParameter("username") final String username, @QueryParameter("password") final String password) {
            try {
                GitHub gh = new GitHubBuilder().withEndpoint(apiUrl).withPassword(username, password).build();
                GHAuthorization token = gh.createToken(asList(GHAuthorization.REPO_STATUS, GHAuthorization.REPO), "Jenkins GitHub Pull Request Plugin", null);
                return FormValidation.ok("Token created: " + token.getToken());
            } catch (IOException ex) {
                return FormValidation.error("Can't create GitHub token " + ex.getMessage());
            }
        }

        private Proxy getProxy() {
            Jenkins instance = getJenkinsInstance();

            Proxy proxy;
            if (instance.proxy == null) {
                proxy = Proxy.NO_PROXY;
            } else {
                proxy = instance.proxy.createProxy(apiUrl);
            }

            return proxy;
        }

        private synchronized void connect() throws IOException {
            Jenkins instance = getJenkinsInstance();

            if (apiUrl == null || apiUrl.isEmpty()) {
                throw new IllegalStateException("GitHub api url is not defined");
            }

            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalStateException("Wrong argument accessToken");
            }

            Cache cache = new Cache(new File(instance.getRootDir(), GitHubPRTrigger.class.getName() + ".cache"), getCacheSize() * 1024 * 1024);
            OkHttpConnector okHttpConnector = new OkHttpConnector(new OkUrlFactory(new OkHttpClient().setCache(cache).setProxy(getProxy())));

            gh = new GitHubBuilder()
                    .withEndpoint(apiUrl)
                    .withRateLimitHandler(RateLimitHandler.FAIL)
                    .withOAuthToken(accessToken)
                    .withConnector(okHttpConnector)
                    .build();

            // don't allow to use connection with bad rate limit or token
            GHRateLimit rateLimit = gh.getRateLimit();
            if (rateLimit.remaining <= 60) {
                gh = null;
                LOGGER.warn(rateLimit.toString());
                throw new IOException("Rate limit is lower then 60, set correct token: " + rateLimit);
            }
        }

        //temp solution for killing connection
        public boolean killConnection() {
            boolean killed = false;

            if (gh != null) {
                gh = null;
                killed = true;
            }

            return killed;
        }

        public GitHub getGitHub() throws IOException {
            if (isConnectionChanged() || gh == null) {
                LOGGER.debug("Opening GitHub connection...");
                connect();
            }
            return gh;
        }

        public boolean isConnectionChanged() {
            boolean changed = false;

            int apiUrlHash = getApiUrl().hashCode();
            int newHash = 31 * apiUrlHash ^ getCacheSize();

            if (getAccessToken() != null) {
                newHash = 31 * newHash ^ getAccessToken().hashCode();
            }

            if (oldHash != newHash) {
                oldHash = newHash;
                changed = true;
                LOGGER.debug("Connection parameters changed");
            }

            return changed;
        }

        public boolean isUserMemberOfOrganization(String organisation, GHUser member) {
            boolean orgHasMember = false;
            try {
                orgHasMember = getGitHub().getOrganization(organisation).hasMember(member);
                LOGGER.debug("org.hasMember(member)? user:'{}' org: '{}' == '{}'",
                        member.getLogin(), organisation, orgHasMember ? "yes" : "no");

            } catch (IOException ex) {
                LOGGER.error("Can't get organization data", ex);
            }
            return orgHasMember;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public String getPublishedURL() {
            return publishedURL;
        }

        public String getJenkinsURL() {
            String url = getPublishedURL();
            if (url != null && !url.trim().equals("")) {
                if (!url.endsWith("/")) {
                    url += "/";
                }
                return url;
            }
            return getJenkinsInstance().getRootUrl();
        }

        public String getWhitelistUserMsg() {
            return whitelistUserMsg;
        }

        public String getSpec() {
            return spec;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        // list all available descriptors for choosing in job configuration
        public List<GitHubPREventDescriptor> getEventDescriptors() {
            return GitHubPREventDescriptor.all();
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

        public static Jenkins getJenkinsInstance() throws IllegalStateException {
            Jenkins instance = Jenkins.getInstance();
            if (instance == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            return instance;
        }

    }
}
