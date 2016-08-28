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

    private final GHBranch ghBranch;
    private final GHRepository repository;
    private final long startTime;

    public GHBranchAppeared(final GHBranch ghBranch) {
        this.ghBranch = ghBranch;
        repository = ghBranch.getOwner();
        startTime = currentTimeMillis();
    }

    @Override
    public Boolean call() throws Exception {
        for (Map.Entry<String, GHBranch> branch : repository.getBranches().entrySet()) {
            if (branch.getKey().equals(ghBranch.getName())) {
                LOG.debug("[WAIT] appeared branch {}, delay {} ms", ghBranch.getName(), currentTimeMillis() - startTime);
                return true;
            }
        }
        LOG.debug("[WAIT] no PR {}", ghBranch.getName());
        return false;
    }

    public static GHBranchAppeared ghBranchAppeared(GHBranch ghBranch) {
        return new GHBranchAppeared(ghBranch);
    }
}
