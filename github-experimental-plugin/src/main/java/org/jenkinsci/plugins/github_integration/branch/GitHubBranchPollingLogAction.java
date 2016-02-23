package org.jenkinsci.plugins.github_integration.branch;

import hudson.model.Job;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchPollingLogAction extends GitHubPRPollingLogAction {
    public GitHubBranchPollingLogAction(Job<?, ?> project) {
        super(project);
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
