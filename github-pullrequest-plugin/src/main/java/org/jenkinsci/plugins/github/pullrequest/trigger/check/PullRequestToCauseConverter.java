package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static com.google.common.base.Predicates.notNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PullRequestToCauseConverter implements Function<GHPullRequest, GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestToCauseConverter.class);

    private final GitHubPRRepository localRepo;
    private final LoggingTaskListenerWrapper listener;
    private final GitHubPRTrigger trigger;

    private PullRequestToCauseConverter(GitHubPRRepository localRepo,
                                        LoggingTaskListenerWrapper listener,
                                        GitHubPRTrigger trigger) {
        this.localRepo = localRepo;
        this.listener = listener;
        this.trigger = trigger;
    }

    public static PullRequestToCauseConverter toGitHubPRCause(GitHubPRRepository localRepo,
                                                              LoggingTaskListenerWrapper listener,
                                                              GitHubPRTrigger trigger) {
        return new PullRequestToCauseConverter(localRepo, listener, trigger);
    }

    /**
     * TODO migrate to java8 and cleanup.
     * @return only real trigger cause if matched trigger (not skip) event found for this remotePr.
     */
    @Override
    public GitHubPRCause apply(final GHPullRequest remotePR) {
        final GitHubPRCause gitHubPRCause = from(trigger.getEvents())
                .transform(toCause(remotePR))
                .filter(notNull())
                .first()
                .orNull();

        if (gitHubPRCause == null || gitHubPRCause.isSkip()) {
            return null;
        } else {
            return gitHubPRCause;
        }
    }

    @VisibleForTesting
    /* package */ EventToCauseConverter toCause(GHPullRequest remotePR) {
        return new EventToCauseConverter(remotePR);
    }

    @VisibleForTesting
    /* package */ class EventToCauseConverter implements Function<GitHubPREvent, GitHubPRCause> {
        private final GHPullRequest remotePR;

        EventToCauseConverter(GHPullRequest remotePR) {
            this.remotePR = remotePR;
        }

        @Override
        public GitHubPRCause apply(GitHubPREvent event) {
            //null if local not existed before
            @CheckForNull GitHubPRPullRequest localPR = localRepo.getPulls().get(remotePR.getNumber());
            try {
                return event.check(trigger, remotePR, localPR, listener);
            } catch (IOException e) {
                LOGGER.warn("Can't check trigger event", e);
                listener.error("Can't check trigger event, so skipping PR ({})", e.getMessage());
                return null;
            }
        }
    }
}
