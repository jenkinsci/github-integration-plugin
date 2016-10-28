package com.github.kostyasha.github.integration.branch.trigger;

import java.util.function.Function;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BranchRepositoryUpdater implements Function<GHBranch, GHBranch> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchRepositoryUpdater.class);

    private final GitHubBranchRepository localRepo;

    private BranchRepositoryUpdater(GitHubBranchRepository localRepo) {
        this.localRepo = localRepo;
    }

    @Override
    public GHBranch apply(GHBranch remoteBranch) {
        LOGGER.trace("Updating local repository with branch [{}]", remoteBranch.getName());
        localRepo.addBranch(new GitHubBranch(remoteBranch));

        return remoteBranch;
    }

    public static BranchRepositoryUpdater branchRepoUpdater(GitHubBranchRepository localRepo) {
        return new BranchRepositoryUpdater(localRepo);
    }
}
