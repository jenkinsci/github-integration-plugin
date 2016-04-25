package org.jenkinsci.plugins.github_integration.awaitility;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class GHRepoAppeared implements Callable<Boolean> {
    private static final Logger LOG = LoggerFactory.getLogger(GHRepoAppeared.class);

    private GitHub gitHub;
    private String repoName;

    public GHRepoAppeared(final GitHub gitHub, final String repoName) {
        this.gitHub = gitHub;
        this.repoName = repoName;
    }

    @Override
    public Boolean call() throws Exception {
        GHRepository repository = gitHub.getRepository(repoName);
        LOG.debug("[WAIT] GitHub repository '{}' {}", repoName, isNull(repository) ? "doesn't appeared" : "appeared");
        return nonNull(repository);
    }

    public static Callable<Boolean> ghRepoAppeared(final GitHub gitHub, final String repoName) {
        return new GHRepoAppeared(gitHub, repoName);
    }
}
