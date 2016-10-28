package com.github.kostyasha.github.integration.branch.trigger;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteBranchToTriggerCause implements Function<GHBranch, GitHubBranchCause> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBranchToTriggerCause.class);

    private final Collection<GitHubBranchEvent> events;

    private GitHubBranchRepository localRepo;

    private LoggingTaskListenerWrapper logTaskWrapper;

    private RemoteBranchToTriggerCause(Collection<GitHubBranchEvent> events, GitHubBranchRepository localRepo,
            LoggingTaskListenerWrapper logTaskWrapper) {
        this.events = events;
        this.localRepo = localRepo;
        this.logTaskWrapper = logTaskWrapper;
    }

    @Override
    public GitHubBranchCause apply(GHBranch remoteBranch) {
        return events.stream()
                .map(event -> toCause(event, remoteBranch))
                .filter(Objects::nonNull)
                .filter(cause -> !cause.isSkip())
                .findFirst()
                .orElse(null);
    }

    private GitHubBranchCause toCause(GitHubBranchEvent event, GHBranch remoteBranch) {
        String branchName = remoteBranch.getName();
        GitHubBranch localBranch = localRepo.getBranch(branchName);

        try {
            return event.check(remoteBranch, localBranch, localRepo, logTaskWrapper);
        } catch (IOException e) {
            LOGGER.error("event check failed, skipping branch [{}]", branchName, e);
            logTaskWrapper.error("event check failed, skipping branch [{}] {}", branchName, e);

            return null;
        }
    }

    public static RemoteBranchToTriggerCause toTriggerCause(Collection<GitHubBranchEvent> events,
            GitHubBranchRepository localRepo, LoggingTaskListenerWrapper logTaskWrapper) {
        return new RemoteBranchToTriggerCause(events, localRepo, logTaskWrapper);
    }
}
