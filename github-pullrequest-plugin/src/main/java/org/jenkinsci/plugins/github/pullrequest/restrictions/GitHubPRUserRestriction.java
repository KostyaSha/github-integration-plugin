package org.jenkinsci.plugins.github.pullrequest.restrictions;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger.DescriptorImpl.githubFor;
import static java.util.Objects.isNull;

/**
 * Whether it allowed this user or org with users to do something.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRUserRestriction implements Describable<GitHubPRUserRestriction> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRUserRestriction.class);
    private final String orgs;
    private final Set<String> orgsSet;
    private final String users;
    private final Set<String> usersSet;

    @DataBoundConstructor
    public GitHubPRUserRestriction(final String orgs, final String users) {
        //TODO check if System.lineSeparator() is correct separator (2 usages)
        this.orgs = orgs;
        this.orgsSet = new HashSet<>(Arrays.asList(orgs.split(System.lineSeparator())));

        this.users = users;
        this.usersSet = new HashSet<>(Arrays.asList(users.split(System.lineSeparator())));
    }

    /**
     * Checks that user is allowed to control
     *
     * @param user commented user
     * @return true if user/org whitelisted
     */
    public boolean isWhitelisted(GHUser user) {
        return !isMyselfUser(user) && usersSet.contains(user.getLogin()) || isInWhitelistedOrg(user);
    }

    public String getOrgs() {
        return orgs;
    }

    public String getUsers() {
        return users;
    }

    public boolean isUserMemberOfOrganization(String organisation, GHUser member) throws IOException {
        boolean orgHasMember;
        //TODO check for null member
        GitHub github = githubFor(URI.create(member.getHtmlUrl().toString()));
        orgHasMember = github.getOrganization(organisation).hasMember(member);
        LOGGER.debug("org.hasMember(member)? user:'{}' org: '{}' == '{}'",
            member.getLogin(), organisation, orgHasMember ? "yes" : "no");

        return orgHasMember;
    }

    private boolean isInWhitelistedOrg(@Nonnull GHUser user) {
        boolean ret = false;
        for (String organisation : orgsSet) {
            try {
                ret = isUserMemberOfOrganization(organisation, user);
                if (ret) {
                    break;
                }
            } catch (IOException e) {
                LOGGER.error("Can't connect retrieve organization data from GitHub", e);
            }
        }
        return ret;
    }

    private static boolean isMyselfUser(GHUser user) {
        boolean ret = false;

        if (isNull(user)) {
            return false;
        }

        try {
            GitHub github = githubFor(URI.create(user.getHtmlUrl().toString()));
            ret = StringUtils.equals(user.getLogin(), github.getMyself().getLogin());
        } catch (IOException e) {
            LOGGER.error("Can't connect retrieve user data from GitHub", e);
        }
        return ret;
    }

    public Descriptor<GitHubPRUserRestriction> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRUserRestriction.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRUserRestriction> {

        @Override
        public final String getDisplayName() {
            return "User restrictions";
        }

    }
}
