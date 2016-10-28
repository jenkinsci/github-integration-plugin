package com.github.kostyasha.github.integration.branch.events;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import hudson.model.AbstractDescribableImpl;
import hudson.model.TaskListener;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubBranchEvent extends AbstractDescribableImpl<GitHubBranchEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchEvent.class);

    /**
     * indicates that branch was created
     * @param remoteBranch        current branch state fetched from GH.
     * @param localBranch         branch state from last run saved in jenkins. null when not exist before.
     * @param localRepo           local repository state. Useful to extract repo URLs for example.
     *
     * @return true if branch should be run. null when no influence.
     */
    @CheckForNull
    public GitHubBranchCause check(
            GHBranch remoteBranch,
            @CheckForNull GitHubBranch localBranch,
            GitHubBranchRepository localRepo,
            TaskListener listener) throws IOException {
        return null;
    }

    @Override
    public GitHubBranchEventDescriptor getDescriptor() {
        return (GitHubBranchEventDescriptor) super.getDescriptor();
    }
}
