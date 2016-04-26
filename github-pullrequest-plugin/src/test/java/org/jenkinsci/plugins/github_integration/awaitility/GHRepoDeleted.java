package org.jenkinsci.plugins.github_integration.awaitility;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.concurrent.Callable;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GHRepoDeleted implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHRepoDeleted.class);

    private GitHub gitHub;
    private String repoName;

    public GHRepoDeleted(final GitHub gitHub, final String repoName) {
        this.gitHub = gitHub;
        this.repoName = repoName;
    }

    @Override
    public Boolean call() throws Exception {
        GHRepository repository;
        try {
            repository = gitHub.getRepository(repoName);
        } catch (FileNotFoundException ignore) {
            LOG.debug("[WAIT] GitHub repository '{}' doesn't found", repoName);
            return true;
        }

        LOG.debug("[WAIT] GitHub repository '{}' {}", repoName, isNull(repository) ? "doesn't found" : "exists");
        return isNull(repository);
    }

    public static Callable<Boolean> ghRepoDeleted(final GitHub gitHub, final String repoName) {
        return new GHRepoDeleted(gitHub, repoName);
    }
}
