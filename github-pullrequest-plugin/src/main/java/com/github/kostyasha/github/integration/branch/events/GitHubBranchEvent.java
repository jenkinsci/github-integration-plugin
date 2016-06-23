package com.github.kostyasha.github.integration.branch.events;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.GitHubLocalBranch;
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
public class GitHubBranchEvent extends AbstractDescribableImpl<GitHubBranchEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchEvent.class);

    /**
     * indicates that PR was changed
     *
     * @param remoteBranch current branch state fetched from GH
     * @param localBranch  branch state from last run saved in jenkins. null when not exist before
     * @return true if PR should be run
     */
    @CheckForNull
    public GitHubBranchCause check(
            GitHubBranchTrigger gitHubPRTrigger,
            GHBranch remoteBranch,
            @CheckForNull GitHubLocalBranch localBranch,
            TaskListener listener) throws IOException {
        return null;
    }

    /**
     * Check that is used for lightweight hooks (pure GitHub hooks).
     */
    public GitHubBranchCause checkHook(GitHubBranchTrigger gitHubPRTrigger,
                                       GHEventPayload payload,
                                       TaskListener listener) {
        return null;
    }

    @Override
    public GitHubBranchEventDescriptor getDescriptor() {
        return (GitHubBranchEventDescriptor) super.getDescriptor();
    }
}
