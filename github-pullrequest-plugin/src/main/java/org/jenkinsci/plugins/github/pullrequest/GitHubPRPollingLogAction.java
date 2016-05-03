package org.jenkinsci.plugins.github.pullrequest;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.github_integration.generic.GitHubAbstractPollingLogAction;

import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Link for project page, shows triggered polling log
 *
 * @author Alina Karpovich
 */
public class GitHubPRPollingLogAction  extends GitHubAbstractPollingLogAction {

    public GitHubPRPollingLogAction(Run<?, ?> run) {
        super(run);
    }

    public GitHubPRPollingLogAction(Job<?, ?> job) {
        super(job);
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR Polling Log";
    }

    @Override
    public String getUrlName() {
        return "github-pr-polling";
    }
}
