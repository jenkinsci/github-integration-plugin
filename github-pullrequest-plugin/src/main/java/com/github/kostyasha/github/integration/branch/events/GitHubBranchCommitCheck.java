package com.github.kostyasha.github.integration.branch.events;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import hudson.model.AbstractDescribableImpl;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare.Commit;

public abstract class GitHubBranchCommitCheck extends AbstractDescribableImpl<GitHubBranchCommitCheck> {

    @Override
    public GitHubBranchCommitCheckDescriptor getDescriptor() {
        return (GitHubBranchCommitCheckDescriptor) super.getDescriptor();
    }

    /**
     * Check used to determine if some associated commit property, such as the commit message, should prevent a build from being triggered.
     *
     * @param remoteBranch current branch state from GH.
     * @param localRepo local repository state.
     * @param commits commits commits that occurred between the last known local hash and the current remote.
     * @return <code>GitHubBranchCause</code> instance indicating if the build should be skipped, <code>null</code> otherwise.
     */
    public abstract GitHubBranchCause check(GHBranch remoteBranch, GitHubBranchRepository localRepo, Commit[] commits);
}
