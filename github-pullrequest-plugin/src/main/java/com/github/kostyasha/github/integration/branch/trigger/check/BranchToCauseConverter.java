package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.google.common.base.Predicates.notNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchToCauseConverter implements Function<GHBranch, GitHubBranchCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchToCauseConverter.class);

    private final GitHubBranchRepository localRepo;
    private final LoggingTaskListenerWrapper listener;
    private final GitHubBranchTrigger trigger;

    private BranchToCauseConverter(GitHubBranchRepository localRepo,
                                   LoggingTaskListenerWrapper listener,
                                   GitHubBranchTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
    }

    public static BranchToCauseConverter toGitHubBranchCause(GitHubBranchRepository localRepo,
                                                             LoggingTaskListenerWrapper listener,
                                                             GitHubBranchTrigger trigger) {
        return new BranchToCauseConverter(localRepo, listener, trigger);
    }

    @Override
    public GitHubBranchCause apply(final GHBranch remoteBranch) {

        final GitHubBranchCause cause = from(trigger.getEvents())
                .transform(toCause(remoteBranch))
                .filter(notNull())
                .first()
                .orNull();

        if (cause == null || cause.isSkip()) {
            return null;
        } else {
            return cause;
        }
    }


    @VisibleForTesting
    /* package */ EventToCauseConverter toCause(GHBranch ghBranch) {
        return new EventToCauseConverter(ghBranch);
    }

    @VisibleForTesting
    /* package */ class EventToCauseConverter implements Function<GitHubBranchEvent, GitHubBranchCause> {
        private final GHBranch remoteBranch;

        EventToCauseConverter(GHBranch remoteBranch) {
            this.remoteBranch = remoteBranch;
        }

        @Override
        public GitHubBranchCause apply(GitHubBranchEvent event) {
            //null if local not existed before
            final GitHubBranch localBranch = localRepo.getBranches().get(remoteBranch.getName());
            try {
                return event.check(trigger, remoteBranch, localBranch, listener);
            } catch (IOException e) {
                LOGGER.warn("Can't check trigger event", e);
                listener.error("Can't check trigger event, so skipping branch ({})", e.getMessage());
                return null;
            }
        }
    }

}
