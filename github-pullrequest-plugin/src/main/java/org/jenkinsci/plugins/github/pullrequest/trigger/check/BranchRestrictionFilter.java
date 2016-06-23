package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class BranchRestrictionFilter implements Predicate<GHPullRequest> {
    private final LoggingTaskListenerWrapper logger;
    private final GitHubPRBranchRestriction branchRestriction;

    private BranchRestrictionFilter(LoggingTaskListenerWrapper logger, GitHubPRBranchRestriction branchRestriction) {
        this.logger = logger;
        this.branchRestriction = branchRestriction;
    }

    public static Predicate<GHPullRequest> withBranchRestriction(
            LoggingTaskListenerWrapper logger, GitHubPRBranchRestriction branchRestriction) {

        if (isNull(branchRestriction)) {
            return Predicates.alwaysTrue();
        }

        return new BranchRestrictionFilter(logger, branchRestriction);
    }

    @Override
    public boolean apply(GHPullRequest remotePR) {
        if (!branchRestriction.isBranchBuildAllowed(remotePR)) {
            logger.info("Skipping [#{} {}] because of com.github.kostyasha.github.integration.branch restriction", remotePR.getNumber(), remotePR.getTitle());
            return false;
        }

        return true;
    }
}
