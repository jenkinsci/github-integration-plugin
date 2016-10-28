package com.github.kostyasha.github.integration.branch.trigger;

import java.util.function.Predicate;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;

/**
 * @author Kanstantsin Shautsou
 */
public class SkipFirstRunForBranchFilter implements Predicate<GHBranch> {

    private final GitHubBranchRepository localRepo;

    private final LoggingTaskListenerWrapper logger;

    private final boolean skipFirstRun;

    private SkipFirstRunForBranchFilter(boolean skipFirstRun, GitHubBranchRepository localRepo,
            LoggingTaskListenerWrapper logger) {
        this.logger = logger;

        this.localRepo = localRepo;
        this.skipFirstRun = skipFirstRun;
    }

    public static SkipFirstRunForBranchFilter skipFirstBuildFilter(boolean skipFirstRun,
            GitHubBranchRepository localRepo, LoggingTaskListenerWrapper logger) {
        return new SkipFirstRunForBranchFilter(skipFirstRun, localRepo, logger);
    }

    @Override
    public boolean test(GHBranch remoteBranch) {
        String name = remoteBranch.getName();
        GitHubBranch localBranch = localRepo.getBranch(name);

        if (skipFirstRun && localBranch == null) {
            logger.info("'skipFirstRun' enabled for first build of branch [{}], skipping", name);
            return false;
        }

        return true;
    }
}
