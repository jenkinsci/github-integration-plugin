package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.filters.GitHubBranchFilter;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BranchFilterToCauseConverter implements Function<GHBranch, GHBranch> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchToCauseConverter.class);

    private final GitHubBranchRepository localRepo;
    private final LoggingTaskListenerWrapper pollingLog;
    private final GitHubBranchTrigger trigger;

    private BranchFilterToCauseConverter(GitHubBranchRepository localRepo,
                                   LoggingTaskListenerWrapper pollingLog,
                                   GitHubBranchTrigger trigger) {
        this.localRepo = localRepo;
        this.pollingLog = pollingLog;
        this.trigger = trigger;
    }

    public static BranchFilterToCauseConverter toGitHubFilterCause(GitHubBranchRepository localRepo,
                                                             LoggingTaskListenerWrapper pollingLog,
                                                             GitHubBranchTrigger trigger) {
        return new BranchFilterToCauseConverter(localRepo, pollingLog, trigger);
    }

    @Override
    public GHBranch apply(GHBranch remoteBranch) {
        List<GitHubBranchCause> causes = trigger.getFilters().stream()
                .map(event -> applyFilter(event, remoteBranch))
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (!causes.isEmpty()) {
            GitHubBranchCause cause = findFilterCause(causes);
            if (cause != null) {
                pollingLog.info("Branch [{}] filtered: {}", name, cause.getReason());
                return null;
            }
        }

        return remoteBranch;
    }

    private GitHubBranchCause findFilterCause(List<GitHubBranchCause> causes) {
        GitHubBranchCause cause = causes.stream()
                .filter(GitHubBranchCause::isSkip)
                .findFirst()
                .orElse(null);

        if (cause == null) {
            return null;
        }

        LOGGER.debug("Cause [{}] indicated build should be skipped", cause);
        return cause;
    }

    private GitHubBranchCause applyFilter(GitHubBranchFilter filter, GHBranch remoteBranch) {
        String branchName = remoteBranch.getName();
        GitHubBranch localBranch = localRepo.getBranches().get(branchName);

        try {
            return filter.check(trigger, remoteBranch, localBranch, localRepo, pollingLog);
        } catch (IOException e) {
            LOGGER.error("Event check failed, skipping branch [{}]", branchName, e);
            pollingLog.error("Event check failed, skipping branch [{}]", branchName);

            return null;
        }
    }
}
