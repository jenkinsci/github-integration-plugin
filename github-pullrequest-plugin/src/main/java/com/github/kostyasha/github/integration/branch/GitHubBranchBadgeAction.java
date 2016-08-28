package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchBadgeAction extends GitHubBadgeAction<GitHubBranchCause> {
    public GitHubBranchBadgeAction(GitHubBranchCause cause) {
        super(cause);
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
