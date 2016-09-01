package com.github.kostyasha.github.integration.branch.trigger.check;

import com.google.common.base.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public class NotUpdatedBranchFilter implements Predicate<GHBranch> {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotUpdatedBranchFilter.class);

    private final GitHubBranchRepository localRepo;
    private final LoggingTaskListenerWrapper logger;

    private NotUpdatedBranchFilter(GitHubBranchRepository localRepo, LoggingTaskListenerWrapper logger) {
        this.localRepo = localRepo;
        this.logger = logger;
    }

    public static NotUpdatedBranchFilter notUpdated(GitHubBranchRepository localRepo, LoggingTaskListenerWrapper logger) {
        return new NotUpdatedBranchFilter(localRepo, logger);
    }

    @Override
    public boolean apply(GHBranch remoteBranch) {
        @CheckForNull GitHubBranch localBranch = localRepo.getBranches().get(remoteBranch.getName());

        if (!isUpdated(remoteBranch, localBranch)) { // light check
            logger.debug("Branch [#{} {}] not changed", remoteBranch.getName(), remoteBranch.getSHA1());
            return false;
        }
        return true;
    }

    /**
     * lightweight check that comments and time were changed
     */
    private static boolean isUpdated(GHBranch remoteBranch, GitHubBranch localBranch) {
        if (isNull(localBranch)) {
            return true; // we don't know yet
        }

        try {
            boolean updated = StringUtils.equals(localBranch.getSHA1(), remoteBranch.getSHA1());

            if (updated) {
                LOGGER.info("Branch #{} was updated by sha: {}",
                        remoteBranch.getName(), remoteBranch.getSHA1());
            }

            return updated;
        } catch (Exception e) {
            // should never happen because
            LOGGER.warn("Can't compare branch [#{} {}] with local copy for update",
                    remoteBranch.getName(), remoteBranch.getSHA1(), e);
            return false;
        }
    }
}
