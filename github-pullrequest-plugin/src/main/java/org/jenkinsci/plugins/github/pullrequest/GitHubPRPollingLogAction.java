package org.jenkinsci.plugins.github.pullrequest;

import com.github.kostyasha.github.integration.generic.GitHubPollingLogAction;
import hudson.model.Job;
import hudson.model.Run;

/**
 * Link for project page, shows triggered polling log
 *
 * @author Alina Karpovich
 */
public class GitHubPRPollingLogAction extends GitHubPollingLogAction {
    public GitHubPRPollingLogAction(Run<?, ?> run) {
        super(run);
    }

    public GitHubPRPollingLogAction(Job<?, ?> job) {
        super(job);
    }

    @Override
    public String getPollingFileName() {
        return "github-pullrequest-polling.log";
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
