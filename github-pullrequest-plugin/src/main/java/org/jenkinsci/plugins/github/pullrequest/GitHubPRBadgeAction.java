package org.jenkinsci.plugins.github.pullrequest;

import hudson.model.BuildBadgeAction;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBadgeAction implements BuildBadgeAction {
    private final String htmlUrl;
    private final String title;
    private final int number;

    public GitHubPRBadgeAction(GitHubPRCause cause) {
        this.htmlUrl = cause.getHtmlUrl().toString();
        this.title = cause.getTitle();
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

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getTitle() {
        return title;
    }

    public int getNumber() {
        return number;
    }
}
