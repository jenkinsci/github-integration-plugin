package org.jenkinsci.plugins.github_integration.awaitility;

import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;

/**
 * @author Kanstantsin Shautsou
 */
public class GHPRAppeared implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHPRAppeared.class);

    private final GHPullRequest pullRequest;
    private final GHRepository repository;
    private final long startTime;

    public GHPRAppeared(final GHPullRequest pullRequest) {
        this.pullRequest = pullRequest;
        repository = pullRequest.getRepository();
        startTime = currentTimeMillis();
    }

    @Override
    public Boolean call() throws Exception {
        for (GHPullRequest pr : repository.listPullRequests(GHIssueState.OPEN)) {
            if (pr.getNumber() == pullRequest.getNumber()) {
                LOG.debug("[WAIT] appeared PR {}, delay {} ms", pullRequest.getNumber(), currentTimeMillis() - startTime);
                return true;
            }
        }
        LOG.debug("[WAIT] no PR {}", pullRequest.getNumber());
        return false;
    }

    public static GHPRAppeared ghPRAppeared(GHPullRequest pullRequest) {
        return new GHPRAppeared(pullRequest);
    }
}
