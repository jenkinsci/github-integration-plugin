package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchBadgeAction extends GitHubBadgeAction<GitHubBranchCause> {

    private String branchName;

    public GitHubBranchBadgeAction(GitHubBranchCause cause) {
        super(cause);
        this.branchName = cause.getBranchName();
    }

    public String getBranchName() {
        return branchName;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }
}
