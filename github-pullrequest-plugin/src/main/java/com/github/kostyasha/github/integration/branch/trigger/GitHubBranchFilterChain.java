package com.github.kostyasha.github.integration.branch.trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.kohsuke.github.GHBranch;

public class GitHubBranchFilterChain implements Predicate<GHBranch> {

    private List<Predicate<GHBranch>> filters = new ArrayList<>();

    private GitHubBranchFilterChain() {
        // builder
    }

    @Override
    public boolean test(GHBranch buildable) {
        return filters.stream().filter(filter -> filter.test(buildable)).findAny().isPresent();
    }

    public GitHubBranchFilterChain with(Predicate<GHBranch> filter) {
        filters.add(filter);
        return this;
    }

    public static GitHubBranchFilterChain filterChain() {
        return new GitHubBranchFilterChain();
    }
}
