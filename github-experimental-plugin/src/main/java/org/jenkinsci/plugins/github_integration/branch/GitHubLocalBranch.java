package org.jenkinsci.plugins.github_integration.branch;

import org.kohsuke.github.GHBranch;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubLocalBranch {
    private String userLogin;

    private String name;
    private String sha1;
//    private String url;


    public GitHubLocalBranch(GHBranch ghBranch) {
        this.name = ghBranch.getName();
        this.sha1 = ghBranch.getSHA1();
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

    public String getUserLogin() {
        return userLogin;
    }
}
