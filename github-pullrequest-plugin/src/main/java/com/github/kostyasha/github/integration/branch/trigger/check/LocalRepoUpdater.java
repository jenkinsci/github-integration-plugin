package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranch;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author Kanstantsin Shautsou
 */
public class LocalRepoUpdater implements Function<GHBranch, GHBranch> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRepoUpdater.class);
    private final GitHubBranchRepository localRepo;

    private LocalRepoUpdater(GitHubBranchRepository localRepo) {
        this.localRepo = localRepo;
    }

    public static LocalRepoUpdater updateLocalRepo(GitHubBranchRepository localRepo) {
        return new LocalRepoUpdater(localRepo);
    }

    @Override
    public GHBranch apply(GHBranch remoteBranch) {
        LOGGER.trace("updating local branch repository with [{}]", remoteBranch.getName());
        localRepo.getBranches().put(remoteBranch.getName(), new GitHubBranch(remoteBranch));

        return remoteBranch;
    }
}
