package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger.DescriptorImpl.getJenkinsInstance;

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
    private static final Logger LOGGER = Logger.getLogger(GitHubPRTrigger.class.getName());

    //TODO replace with {@link GitHubRepositoryName.class} ?
    private static final Pattern ghFullRepoName = Pattern.compile("^(http[s]?://[^/]*)/([^/]*/[^/]*).*");

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
    private transient GitHubPRRepository localRepository;
    private GitHubPRPollingLogAction pollingLogAction;

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
        LOGGER.log(Level.INFO, "Starting GitHub Pull Request trigger for project {0}", project.getName());

        if (getTriggerMode() != GitHubPRTriggerMode.CRON) {
            //TODO implement
            return;
        }

        super.start(project, newInstance);
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
            LOGGER.log(Level.SEVERE, "job object is null, race condition?");
            throw new IllegalStateException("Job object is null");
        }

        if (job.getProperty(GithubProjectProperty.class) == null) {
            LOGGER.log(Level.INFO, "GitHub project not set up, cannot start GitHub PR trigger for job {0}", job);
            throw new IllegalArgumentException("GitHub project property is not defined. " +
                    "Cannot start GitHub PR trigger for job " + job.getName());
        }

        final GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);
        if (ghpp == null || ghpp.getProjectUrl() == null) {
            throw new IllegalArgumentException("A GitHub project url is required.");
        }

        String baseUrl = ghpp.getProjectUrl().baseUrl();
        Matcher m = ghFullRepoName.matcher(baseUrl);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid GitHub project url: %s", baseUrl));
        }

        repoFullName = m.group(2);
        return repoFullName;
    }

    @Override
    public void run() {
        if (job == null || job.isDisabled()) {
            LOGGER.log(Level.FINE, "Job {0} is disabled, but trigger run!", job == null ? "no job" : job.getFullName());
            return;
        }

        // triggers are always triggered on the cron, but we just no-op if we are using GitHub hooks.
        if (getTriggerMode() != GitHubPRTriggerMode.CRON) {
            return;
        }

        long startTime = System.currentTimeMillis();

        List<GitHubPRCause> causes = Collections.emptyList();
        try (StreamTaskListener listener = new StreamTaskListener(pollingLogAction.getLogFile())) {
            final PrintStream logger = listener.getLogger();
            logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
            LOGGER.log(Level.FINE, "Running GitHub Pull Request trigger check.");

            GitHubPRRepository localRepository = null;
            try {
                localRepository = job.getAction(GitHubPRRepository.class);
                causes = check(localRepository, listener);
            } catch (IOException e) {
                listener.error("Can't save repository state, because " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Can't save repository state, because: '{0}'", e.getMessage());
            }

            if (localRepository != null) {
                try {
                    localRepository.save();
                } catch (IOException e) {
                    listener.error("Can't save repository state, because " + e.getMessage());
                    LOGGER.log(Level.SEVERE, "Can't save repository state, because: '{0}'", e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            LOGGER.log(Level.INFO, "End  GitHub Pull Request trigger check. Summary time: {0}ms", duration);
            logger.println("Finished at " + DateFormat.getDateTimeInstance().format(new Date())
                    + ", duration " + duration + "ms");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "can't trigger build");
        }

        for (GitHubPRCause cause : causes) {
            try {
                cause.setPollingLog(pollingLogAction.getLogFile());
                build(cause);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "can't trigger build");
            }
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        pollingLogAction = new GitHubPRPollingLogAction(job);
        return Collections.singleton(pollingLogAction);
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
        LOGGER.log(Level.FINE, "GitHub rate limit before check: {0}", rateLimitBefore);
        logger.println("GitHub rate limit before check: " + rateLimitBefore);
        int checkedPR = 0;

        final ArrayList<GitHubPRCause> gitHubPRCauses = new ArrayList<GitHubPRCause>();

        // get local and remote list of PRs
        HashMap<Integer, GitHubPRPullRequest> localPulls = localRepository.getPulls();
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
                LOGGER.log(Level.FINE, "PR #{0} {1} not changed", new Object[]{remotePR.getNumber(),
                        remotePR.getTitle()});
                logger.println("PR #" + remotePR.getNumber() + " " + remotePR.getTitle() + " not changed");
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
                LOGGER.log(Level.INFO, "Skipping first run for {0} and PR #{1}",
                        new Object[]{job.getFullName(), remotePR.getNumber()});
                logger.println("Skipping first run for " + job.getFullName() + " and PR #" + remotePR.getNumber());
                continue;
            }

            if (branchRestriction != null && branchRestriction.isBranchBuildAllowed(remotePR)) {
                LOGGER.log(Level.WARNING, "Skipping #{0} {1} because of branch restriction",
                        new Object[]{remotePR.getNumber(), remotePR.getTitle()});
                logger.println("Skipping #" + remotePR.getNumber() + " " + remotePR.getTitle() + " because of branch restriction");
                continue;
            }

            if (userRestriction != null && !userRestriction.isWhitelisted(remotePR.getUser())) {
                LOGGER.log(Level.WARNING, "Skipping #{0} {1} because of user restriction (user - {2})",
                        new Object[]{remotePR.getNumber(), remotePR.getTitle(), remotePR.getUser()});
                logger.println("Skipping #" + remotePR.getNumber() + " " + remotePR.getTitle()
                        + " because of user restriction (user - " + remotePR.getUser() + ")");
                continue;
            }

            for (GitHubPREvent event : getEvents()) {  // waterfall, first matched win
                try {
                    GitHubPRCause cause = event.check(this, remotePR, localPR, listener);
                    if (cause != null) {
                        if (cause.isSkip()) {
                            LOGGER.log(Level.FINE, "Skipping PR #{0}", remotePR.getNumber());
                            logger.println("Skipping PR #" + remotePR.getNumber());
                            break;
                        } else {
                            LOGGER.log(Level.FINE, "Triggering build for PR #'{0}', because {1}",
                                    new Object[]{remotePR.getNumber(), cause.getReason()});
                            logger.println("Triggering build for PR #" + remotePR.getNumber() + " because " + cause.getReason());
                            gitHubPRCauses.add(cause);
                            // don't check other events
                            break;
                        }
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Can't check trigger event", e);
                    listener.error("Skip event failed, so skipping PR");
                    break;
                }

            }
        }


        if (skipFirstRun) {
            LOGGER.log(Level.INFO, "Skipping first run for {0}", job.getFullName());
            skipFirstRun = false;
            trySave(); //TODO or better fail with IOException?
        }

        GHRateLimit rateLimitAfter = getGitHub().getRateLimit();
        int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
        LOGGER.log(Level.INFO, "GitHub rate limit after check: {0}, consumed: {1}, checked PRs: {2}",
                new Object[]{rateLimitAfter, consumed, checkedPR});
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
            LOGGER.log(Level.INFO, "Pull request #{0} was updated at: {1} by {2}",
                    new Object[]{localPR.getNumber(), localPR.getPrUpdatedAt(), localPR.getUserLogin()});
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
            LOGGER.log(Level.SEVERE, "Job didn't start");
        }

        LOGGER.log(Level.INFO, sb.toString());

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
            LOGGER.log(Level.INFO, "Stopping the GitHub PR trigger for project {0}", job.getFullName());
        }
        super.stop();
    }

    private QueueTaskFuture<?> startJob(GitHubPRCause cause) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new CauseAction(cause));

        List<ParameterValue> values = getDefaultParametersValues();

        values.add(new StringParameterValue("GITHUB_PR_TRIGGER_SENDER_AUTHOR", valueOf(cause.getTriggerSenderName())));
        values.add(new StringParameterValue("GITHUB_PR_TRIGGER_SENDER_EMAIL", valueOf(cause.getTriggerSenderEmail())));

        values.add(new StringParameterValue("GITHUB_PR_COMMIT_AUTHOR_NAME", valueOf(cause.getCommitAuthorName())));
        values.add(new StringParameterValue("GITHUB_RP_COMMIT_AUTHOR_EMAIL", valueOf(cause.getCommitAuthorEmail())));

        values.add(new StringParameterValue("GITHUB_PR_TARGET_BRANCH", valueOf(cause.getTargetBranch())));
        values.add(new StringParameterValue("GITHUB_PR_SOURCE_BRANCH", valueOf(cause.getSourceBranch())));

        values.add(new StringParameterValue("GITHUB_PR_AUTHOR_EMAIL", valueOf(cause.getPRAuthorEmail())));

        values.add(new StringParameterValue("GITHUB_PR_SHORT_DESC", valueOf(cause.getShortDescription())));
        values.add(new StringParameterValue("GITHUB_PR_TITLE", valueOf(cause.getTitle())));
        values.add(new StringParameterValue("GITHUB_PR_URL", valueOf(cause.getHtmlUrl().toString())));
        values.add(new StringParameterValue("GITHUB_PR_SOURCE_REPO_OWNER", valueOf(cause.getSourceRepoOwner())));
        values.add(new StringParameterValue("GITHUB_PR_HEAD_SHA", cause.getHeadSha()));
        values.add(new StringParameterValue("GITHUB_PR_COND_REF", cause.getCondRef()));  //TODO better name?
        values.add(new BooleanParameterValue("GITHUB_PR_CAUSE_SKIP", cause.isSkip()));
        final StringParameterValue prNumber = new StringParameterValue("GITHUB_PR_NUMBER", valueOf(Integer.toString(cause.getNumber())));
        values.add(prNumber);

        actions.add(new ParametersAction(values));

        return this.job.scheduleBuild2(job.getQuietPeriod(), null, actions);
    }

    private String valueOf(String s) {
        return s == null ? "" : s;
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
        ArrayList<ParameterValue> defValues = new ArrayList<>();

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
            LOGGER.log(Level.INFO, "Can't connect to GitHub {0}. Bad Global plugin configuration.", ex.getMessage());
            throw new IOException("Can't connect to GitHub: " + ex.getMessage() + ". Bad Global plugin configuration.");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Can't connect to GitHub {0}. Bad Global plugin configuration.", ex.getMessage());
            throw ex;
        } catch (Throwable t) {
            LOGGER.log(Level.FINE, "Can't connect to GitHub {0}. Bad Global plugin configuration.", t.getMessage());
            throw new IOException("Can't connect to GitHub: " + t.getMessage() + ". Bad Global plugin configuration.");
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
            LOGGER.log(Level.SEVERE, "Error while saving job to file", e);
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
        // GitHub username may only contain alphanumeric characters or dashes and cannot begin with a dash
        private static final Pattern adminlistPattern = Pattern.compile("((\\p{Alnum}[\\p{Alnum}-]*)|\\s)*");
        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());

        private String apiUrl = "https://api.github.com";
        private String whitelistUserMsg = ".*add\\W+to\\W+whitelist.*";
        private String testPhrase = ".*test\\W+this\\W+please.*";
        private String skipBuildPhrase = ".*\\[skip\\W+ci\\].*";
        private String spec = "H/5 * * * *";
        private String msgSuccess = "Test PASSed.";
        private String msgFailure = "Test FAILed.";
        private String username;
        private String password;
        private String accessToken;
        private String publishedURL;
        private String requestForTestingPhrase;
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
            requestForTestingPhrase = formData.getString("requestForTestingPhrase");
            whitelistUserMsg = formData.getString("whitelistUserMsg");
            testPhrase = formData.getString("testPhrase");
            skipBuildPhrase = formData.getString("skipBuildPhrase");
            spec = formData.getString("spec");
            msgSuccess = formData.getString("msgSuccess");
            msgFailure = formData.getString("msgFailure");
            cacheSize = formData.getInt("cacheSize");

            save();
            return super.configure(req, formData);
        }

        public FormValidation doCheckAdminlist(@QueryParameter String value) throws ServletException {
            if (!adminlistPattern.matcher(value).matches()) {
                return FormValidation.error("GitHub username may only contain alphanumeric characters or dashes and cannot begin with a dash. Separate them with whitespaces.");
            }
            return FormValidation.ok();
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
                GHAuthorization token = gh.createToken(Arrays.asList(GHAuthorization.REPO_STATUS, GHAuthorization.REPO), "Jenkins GitHub Pull Request Plugin", null);
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
                LOGGER.log(Level.WARNING, rateLimit.toString());
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
                LOGGER.log(Level.FINE, "Opening GitHub connection...");
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
                LOGGER.log(Level.FINE, "Connection parameters changed");
            }

            return changed;
        }

        public boolean isUserMemberOfOrganization(String organisation, GHUser member) {
            boolean orgHasMember = false;
            try {
                orgHasMember = getGitHub().getOrganization(organisation).hasMember(member);
                LOGGER.log(Level.FINE, "org.hasMember(member)? user:'{0}' org: '{1}' == '{2}'",
                        new Object[]{member.getLogin(), organisation, orgHasMember ? "yes" : "no"});

            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Can't get organization data", ex);
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

        public String getRequestForTestingPhrase() {
            return requestForTestingPhrase;
        }

        public String getWhitelistUserMsg() {
            return whitelistUserMsg;
        }

        public String getTestPhrase() {
            return testPhrase;
        }

        public String getSkipBuildPhrase() {
            return skipBuildPhrase;
        }

        public String getSpec() {
            return spec;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public String getMsgSuccess() {
            return msgSuccess;
        }

        public String getMsgFailure() {
            return msgFailure;
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
