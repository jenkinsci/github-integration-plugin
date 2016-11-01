package com.github.kostyasha.github.integration.branch.trigger.check;


import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;

import java.util.function.Predicate;

/**
 * @author Kanstantsin Shautsou
 */
public class SkipFirstRunForBranchFilter implements Predicate<GHBranch> {
    private final LoggingTaskListenerWrapper logger;
    private final boolean skipFirstRun;

    private SkipFirstRunForBranchFilter(LoggingTaskListenerWrapper logger, boolean skipFirstRun) {
        this.logger = logger;
        this.skipFirstRun = skipFirstRun;
    }

    public static Predicate<GHBranch> ifSkippedFirstRun(LoggingTaskListenerWrapper logger, boolean skipFirstRun) {
        return new SkipFirstRunForBranchFilter(logger, skipFirstRun);
    }

    @Override
    public boolean test(GHBranch remoteBranch) {
        if (skipFirstRun) {
            logger.info("Skipping first run for branch '{}'", remoteBranch.getName());
            return false;
        }

        return true;
    }
}
