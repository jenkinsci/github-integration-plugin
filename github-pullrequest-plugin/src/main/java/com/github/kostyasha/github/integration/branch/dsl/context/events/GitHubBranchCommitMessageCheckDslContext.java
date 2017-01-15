package com.github.kostyasha.github.integration.branch.dsl.context.events;

import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitMessageCheck;

import java.util.LinkedHashSet;
import java.util.Set;

import javaposse.jobdsl.dsl.Context;

public class GitHubBranchCommitMessageCheckDslContext implements Context {
    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final Set<String> matchCriteria = new LinkedHashSet<>();
    private GitHubBranchCommitMessageCheck event = new GitHubBranchCommitMessageCheck();

    public void excludeMatching() {
        event.setExclude(true);
    }

    public void matchCritieria(String matchCritieria) {
        this.matchCriteria.add(matchCritieria);
    }

    public GitHubBranchCommitMessageCheck getCheck() {
        event.setMatchCriteria(String.join(LINE_SEPARATOR, matchCriteria));
        return event;
    }
}
