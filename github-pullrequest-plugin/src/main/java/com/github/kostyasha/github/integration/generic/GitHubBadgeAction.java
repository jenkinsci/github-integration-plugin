package com.github.kostyasha.github.integration.generic;

import hudson.matrix.MatrixChildAction;
import hudson.model.BuildBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubBadgeAction<T extends GitHubCause<T>> implements BuildBadgeAction, MatrixChildAction {
    protected String htmlUrl;
    protected String title;

    public GitHubBadgeAction(T cause) {
        this.htmlUrl = cause.getHtmlUrl().toString();
        this.title = cause.getTitle();
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getTitle() {
        return title;
    }

}
