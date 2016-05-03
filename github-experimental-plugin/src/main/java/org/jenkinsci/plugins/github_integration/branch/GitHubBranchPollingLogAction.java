package org.jenkinsci.plugins.github_integration.branch;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.github_integration.generic.GitHubAbstractPollingLogAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchPollingLogAction extends GitHubAbstractPollingLogAction {
    public GitHubBranchPollingLogAction(Job<?, ?> job) {
        super(job);
    }

    public GitHubBranchPollingLogAction(Run run) {
        super(run);
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "GitHub Push Polling Log";
    }

    @Override
    public String getUrlName() {
        return "github-push-polling";
    }

    public String getPollingFileName() {
        return "github-push-polling.log";
    }
}
