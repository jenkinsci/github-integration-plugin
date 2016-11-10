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

    public abstract GitHubBranchCause check(GHBranch remoteBranch, GitHubBranchRepository localRepo,
            Commit[] commits);
}
