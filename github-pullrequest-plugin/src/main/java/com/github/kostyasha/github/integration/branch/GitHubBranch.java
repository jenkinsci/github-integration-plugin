package com.github.kostyasha.github.integration.branch;

import hudson.Functions;
import org.kohsuke.github.GHBranch;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranch {

    private String name;
    private String sha1;
    private String htmlUrl;

    public GitHubBranch(GHBranch ghBranch) {
        name = ghBranch.getName();
        sha1 = ghBranch.getSHA1();
        htmlUrl = ghBranch.getOwner().getHtmlUrl().toString() + "/tree/" + name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSHA1() {
        return sha1;
    }

    public void setSHA1(String sha1) {
        this.sha1 = sha1;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public static String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-branch.svg";
    }
}
