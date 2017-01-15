package com.github.kostyasha.github.integration.branch.dsl.context.events;


import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheck;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitChecks;

import java.util.ArrayList;
import java.util.List;

import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

public class GitHubBranchCommitChecksDslContext implements Context {

    private GitHubBranchCommitChecks check = new GitHubBranchCommitChecks();
    private List<GitHubBranchCommitCheck> checks = new ArrayList<>();

    public void commitMessagePattern(Runnable closure) {
        GitHubBranchCommitMessageCheckDslContext commitContext = new GitHubBranchCommitMessageCheckDslContext();
        ContextExtensionPoint.executeInContext(closure, commitContext);

        checks.add(commitContext.getCheck());
    }

    public GitHubBranchCommitChecks getCheck() {
        check.setChecks(checks);
        return check;
    }
}
