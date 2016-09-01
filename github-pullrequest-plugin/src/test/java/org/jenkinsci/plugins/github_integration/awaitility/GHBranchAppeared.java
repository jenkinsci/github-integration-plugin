package org.jenkinsci.plugins.github_integration.awaitility;

import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Callable;

import static java.lang.System.currentTimeMillis;

/**
 * @author Kanstantsin Shautsou
 */
public class GHBranchAppeared implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHPRAppeared.class);

    private final String branchName;
    private final GHRepository repository;
    private final long startTime;

    public GHBranchAppeared(final GHRepository repository, String branchName) {
        this.branchName = branchName;
        this.repository = repository;
        startTime = currentTimeMillis();
    }

    @Override
    public Boolean call() throws Exception {
        for (Map.Entry<String, GHBranch> entry : repository.getBranches().entrySet()) {
            if (entry.getKey().equals(branchName)) {
                LOG.debug("[WAIT] appeared branch {}, delay {} ms", branchName, currentTimeMillis() - startTime);
                return true;
            }
        }
        LOG.debug("[WAIT] no Branch {}", branchName);
        return false;
    }

    public static GHBranchAppeared ghBranchAppeared(GHRepository repository, String branchName) {
        return new GHBranchAppeared(repository, branchName);
    }
}
