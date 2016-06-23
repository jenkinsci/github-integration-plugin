package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
public class PushToCauseConverter implements Function<GHBranch, GitHubBranchCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushToCauseConverter.class);

    private final GitHubBranchRepository localRepo;
    private final LoggingTaskListenerWrapper listener;
    private final GitHubBranchTrigger trigger;

    private PushToCauseConverter(GitHubBranchRepository localRepo,
                                 LoggingTaskListenerWrapper listener,
                                 GitHubBranchTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
    }

    public static PushToCauseConverter toGitHubPushCause(GitHubBranchRepository localRepo,
                                                       LoggingTaskListenerWrapper listener,
                                                       GitHubBranchTrigger trigger) {
        return new PushToCauseConverter(localRepo, listener, trigger);
    }

    @Override
    public GitHubBranchCause apply(final GHBranch remoteBranch) {
        return new GitHubBranchCause(remoteBranch);
    }

}
