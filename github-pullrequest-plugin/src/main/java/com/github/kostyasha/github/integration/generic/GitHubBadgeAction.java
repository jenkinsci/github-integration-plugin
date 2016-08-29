package com.github.kostyasha.github.integration.generic;

import hudson.matrix.MatrixChildAction;
import hudson.model.BuildBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubBadgeAction<T extends GitHubCause<T>> implements BuildBadgeAction, MatrixChildAction {
    private String htmlUrl;
    private String title;

    public GitHubBadgeAction(GitHubCause cause) {
        this.htmlUrl = cause.getHtmlUrl().toString();
        this.title = cause.getTitle();
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    /**
     * @see #htmlUrl
     */
    public GitHubBadgeAction withHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @see #title
     */
    public GitHubBadgeAction withTitle(String title) {
        this.title = title;
        return this;
    }
}
