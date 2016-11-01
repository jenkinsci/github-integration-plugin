package com.github.kostyasha.github.integration.branch.filters;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;

import hudson.model.AbstractDescribableImpl;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;

import java.io.IOException;

public abstract class GitHubBranchFilter extends AbstractDescribableImpl<GitHubBranchFilter> {
    public abstract GitHubBranchCause check(GitHubBranchTrigger trigger, GHBranch remoteBranch, GitHubBranch localBranch,
            GitHubBranchRepository localRepo, LoggingTaskListenerWrapper pollingLog) throws IOException;
}
