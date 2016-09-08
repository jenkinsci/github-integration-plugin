package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;
import hudson.Extension;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * Triggers when branch didn't exist before and appeared in remote.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCreatedEvent extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Branch Created";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCreatedEvent.class);

    @DataBoundConstructor
    public GitHubBranchCreatedEvent() {
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger trigger,
                                   GHBranch remoteBranch,
                                   @CheckForNull GitHubBranch localBranch,
                                   TaskListener listener) throws IOException {
        if (remoteBranch == null && localBranch == null) {
            // just in case
            LOG.debug("Remote and local branch are null");
            return null;
        } else if (remoteBranch == null) {
            LOG.debug("Remote branch is null for localBranch '{}'. Branch can't be 'created'", localBranch.getName());
            return null;
        }

        GitHubBranchCause cause = null;
        if (isNull(localBranch)) { // didn't exist before
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": state has changed (branch was created)");
            LOG.debug("{}: state has changed (branch was created)", DISPLAY_NAME);
            cause = new GitHubBranchCause(remoteBranch, DISPLAY_NAME, false);
        }

        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
