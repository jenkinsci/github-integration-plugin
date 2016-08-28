package org.jenkinsci.plugins.github.pullrequest;

import com.github.kostyasha.github.integration.generic.GitHubBadgeAction;
import hudson.matrix.MatrixChildAction;
import hudson.model.BuildBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBadgeAction extends GitHubBadgeAction<GitHubPRCause> {

    private final int number;

    public GitHubPRBadgeAction(GitHubPRCause cause) {
        super(cause);
        this.number = cause.getNumber();
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

    public int getNumber() {
        return number;
    }
}
