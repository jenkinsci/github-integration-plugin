package org.jenkinsci.plugins.github.pullrequest.restrictions;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRUserRestriction implements Describable<GitHubPRUserRestriction> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRUserRestriction.class);
    private final Set<String> orgsSet;
    private final Set<String> usersSet;
    private final String whitelistUserMsg;
    private final String orgs;

    private final String users;
    private transient volatile Pattern whitelistUserPattern;

    @DataBoundConstructor
    public GitHubPRUserRestriction(final String orgs, final String users, final String whitelistUserMsg) {
        //TODO check if System.lineSeparator() is correct separator (2 usages)
        this.orgs = orgs;
        this.orgsSet = new HashSet<>(Arrays.asList(orgs.split(System.lineSeparator())));
        this.users = users;
        this.usersSet = new HashSet<>(Arrays.asList(users.split(System.lineSeparator())));
        this.whitelistUserMsg = whitelistUserMsg;
    }

    /**
     * Add/remove users/orgs internal state
     */
    public void updateUsers(GHPullRequest remotePr) throws IOException {
        PagedIterable<GHIssueComment> ghIssueComments = remotePr.listComments();
        //TODO check
        for (GHIssueComment comment : ghIssueComments) {
            if (whitelistUserPattern == null) {
                whitelistUserPattern = Pattern.compile(whitelistUserMsg);
            }

        }
    }

    public void addUserToWhitelist(String author, GitHubPRTrigger gitHubPRTrigger) {
        LOGGER.info("Adding {} to whitelist", author);
        usersSet.add(author);
        gitHubPRTrigger.trySave();
    }


    /**
     * Checks that user is allowed to control
     *
     * @param user commented user
     *
     * @return true if user/org whitelisted
     */
    public boolean isWhitelisted(GHUser user) {
        return !isMyselfUser(user) && usersSet.contains(user.getLogin()) || isInWhitelistedOrg(user);
    }

    /**
     * Method that updates local whitelist with new users.
     */
    public void populate(GHPullRequest remotePR, GitHubPRPullRequest localPR, GitHubPRTrigger gitHubPRTrigger) {
        try {
            for (GHIssueComment remoteComment : remotePR.getComments()) {
                String remoteCommentBody = remoteComment.getBody();
                if (localPR.getLastCommentCreatedAt().compareTo(remoteComment.getCreatedAt()) < 0) {
                    //this remote comment is new
                    GHUser remoteCommentAuthor = remoteComment.getUser();
                    if (!isMyselfUser(remoteCommentAuthor) && whitelistUserPattern.matcher(remoteCommentBody).matches()) {
                        /* author is not bot, he is admin and there is code phrase for whitelisting users
                         * in the comment body, so add PR author to whitelist*/
                        addUserToWhitelist(remotePR.getUser().getName(), gitHubPRTrigger);
                    }
                }
            }

        } catch (IOException e) {
            LOGGER.error("Can't connect retrieve comment data from GitHub", e);
        }
    }

    public String getWhitelistUserMsg() {
        return whitelistUserMsg;
    }

    public String getOrgs() {
        return orgs;
    }

    public String getUsers() {
        return users;
    }

    private boolean isMyselfUser(GHUser user) {
        boolean ret = false;
        
        if(user == null) {
            return false;
        }
        
        try {
            GitHub github = GitHubPRTrigger.DescriptorImpl.githubFor(URI.create(user.getHtmlUrl().toString()));
            ret = StringUtils.equals(user.getLogin(), github.getMyself().getLogin());
        } catch (IOException e) {
            LOGGER.error("Can't connect retrieve user data from GitHub", e);
        }
        return ret;
    }

    public boolean isUserMemberOfOrganization(String organisation, GHUser member) {
        boolean orgHasMember = false;
        try {
            //TODO check for null member
            GitHub github = GitHubPRTrigger.DescriptorImpl.githubFor(URI.create(member.getHtmlUrl().toString()));
            orgHasMember = github.getOrganization(organisation).hasMember(member);
            LOGGER.debug("org.hasMember(member)? user:'{}' org: '{}' == '{}'",
                    member.getLogin(), organisation, orgHasMember ? "yes" : "no");

        } catch (IOException ex) {
            LOGGER.error("Can't get organization data", ex);
        }
        return orgHasMember;
    }

    private boolean isInWhitelistedOrg(GHUser user) {
        boolean ret = false;
        for (String organisation : orgsSet) {
            try {
                //TODO check for null user
                GitHub github = GitHubPRTrigger.DescriptorImpl.githubFor(URI.create(user.getHtmlUrl().toString()));
                GHOrganization ghOrganization = github.getOrganization(organisation);
                ret = ghOrganization.hasMember(user);
                if (ret) {
                    break;
                }
            } catch (IOException e) {
                LOGGER.error("Can't connect retrieve organization data from GitHub", e);
            }
        }
        return ret;
    }

    public Descriptor<GitHubPRUserRestriction> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRUserRestriction.class);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRUserRestriction> {

        public String getWhitelistUserMsg() {
            return GitHubPRTrigger.DescriptorImpl.get().getWhitelistUserMsg();
        }

        @Override
        public final String getDisplayName() {
            return "User restrictions";
        }

    }
}
