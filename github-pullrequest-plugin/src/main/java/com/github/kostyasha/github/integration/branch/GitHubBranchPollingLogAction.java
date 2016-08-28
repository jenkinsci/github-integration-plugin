package com.github.kostyasha.github.integration.branch;

import hudson.model.Job;
import hudson.model.Run;
import com.github.kostyasha.github.integration.generic.GitHubPollingLogAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchPollingLogAction extends GitHubPollingLogAction {
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
        return "GitHub Branch Polling Log";
    }

    @Override
    public String getUrlName() {
        return "github-branch-polling";
    }

    public String getPollingFileName() {
        return "github-branch-polling.log";
    }
}
