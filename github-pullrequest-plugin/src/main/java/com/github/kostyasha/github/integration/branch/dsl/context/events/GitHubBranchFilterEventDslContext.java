package com.github.kostyasha.github.integration.branch.dsl.context.events;

import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchRestrictionFilter;

import java.util.LinkedHashSet;
import java.util.Set;

import javaposse.jobdsl.dsl.Context;

public class GitHubBranchFilterEventDslContext implements Context {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final GitHubBranchRestrictionFilter filter = new GitHubBranchRestrictionFilter();
    private final Set<String> matchCriteria = new LinkedHashSet<>();

    public void excludeMatching() {
        filter.setExclude(true);
    }

    public void matchAgainstPatterns() {
        filter.setMatchAsPattern(true);
    }

    public void matchCritieria(String matchCritieria) {
        this.matchCriteria.add(matchCritieria);
    }

    public GitHubBranchRestrictionFilter getFilter() {
        filter.setMatchCriteriaStr(String.join(LINE_SEPARATOR, matchCriteria));
        return filter;
    }
}
