package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.io.IOUtils;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook handler for GitHubPR.
 * @author Kanstantsin Shautsou
 * @author Alina Karpovich
 */
@Extension
public class GitHubPRRootAction implements UnprotectedRootAction {
    static final String URL = "github-pullrequest";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRootAction.class);

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return URL;
    }

    @RequirePOST
    public void doIndex(StaplerRequest req, StaplerResponse resp) {
        String event = req.getHeader("X-GitHub-Event");
        if (event.equals("ping")) {
            LOGGER.info("Got 'ping' event");
        } else {
            LOGGER.debug("Got {} event", event);
        }

        try {
            GitHubPRTrigger.DescriptorImpl descriptor = (GitHubPRTrigger.DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(GitHubPRTrigger.class);
            GitHub gh = descriptor.getGitHub();

            String type = req.getContentType();
            String payload = null;
            if ("application/json".equals(type)) {
                try (BufferedReader reader = req.getReader()) {
                    payload = IOUtils.toString(reader);
                } catch (IOException e) {
                    LOGGER.error("Can't get payload from json: {}", e.getMessage());
                    return;
                }
            } else if ("application/x-www-form-urlencoded".equals(type)) {
                payload = req.getParameter("payload");
            } else {
                LOGGER.error("Unknown content type {}", type);
                return;
            }

            if (payload == null || payload.isEmpty()) {
                LOGGER.warn("Bad payload {}", payload);
                return;
            } else {
                LOGGER.trace("Payload {}", payload);
            }


            GHEventPayload parsedPayload = null;
            Set<AbstractProject<?, ?>> jobs = new HashSet<>();
            if ("issue_comment".equals(event)) {
                parsedPayload = gh.parseEventPayload(new StringReader(payload), GHEventPayload.IssueComment.class);
                GHEventPayload.IssueComment commentPayload = gh.parseEventPayload(new StringReader(payload), GHEventPayload.IssueComment.class);
                jobs = getJobs(commentPayload.getRepository().getFullName());
            } else if ("pull_request".equals(event)) {
                parsedPayload = gh.parseEventPayload(new StringReader(payload), GHEventPayload.PullRequest.class);
                GHEventPayload.PullRequest pr = (GHEventPayload.PullRequest) parsedPayload;
                jobs = getJobs(pr.getPullRequest().getRepository().getFullName());
            } else {
                LOGGER.warn("Request not known");
            }

            for (AbstractProject<?, ?> job : jobs) {
                GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
                if (trigger == null) {
                    continue;
                }

                if (trigger.getTriggerMode() == null) {
                    LOGGER.warn("Job {} has bad trigger mode.", job.getFullName());
                    continue;
                }

                switch (trigger.getTriggerMode()) {
                    case HEAVY_HOOKS:
                        trigger.queueRun(job);
                        break;

                    case LIGHT_HOOKS:
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
//                        LOGGER.log(Level.INFO, "Begin processing hooks for {0}", trigger.getRepoFullName(job));
//                        for (GitHubPREvent prEvent : trigger.getEvents()) {
//                            GitHubPRCause cause = prEvent.checkHook(trigger, parsedPayload, null);
//                            if (cause != null) {
//                                trigger.build(cause);
//                            }
//                        }
                        break;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to parse github hook payload.", ex);
        }
    }

    /**
     * get all jobs (AbstractProject objects) for repository specified by name
     */
    private Set<AbstractProject<?, ?>> getJobs(String repo) {
        final Set<AbstractProject<?, ?>> ret = new HashSet<>();

        // We need this to get access to list of repositories
        Authentication old = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);

        try {
            for (AbstractProject<?, ?> job : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                if (!job.isBuildable()) {
                    continue;
                }

                GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
                if (trigger != null && trigger.getTriggerMode() != null && repo.equalsIgnoreCase(trigger.getRepoFullName(job))) {
                    ret.add(job);
                }
            }
        } finally {
            SecurityContextHolder.getContext().setAuthentication(old);
        }

        return ret;
    }
}
