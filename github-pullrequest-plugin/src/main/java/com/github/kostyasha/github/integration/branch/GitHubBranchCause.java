package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import hudson.model.Run;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.apache.commons.io.FileUtils.writeStringToFile;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCause extends GitHubCause<GitHubBranchCause> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchCause.class);

    private final String branchName;
    private final String headSha;

    public GitHubBranchCause(GHBranch remoteBranch) {
        this.branchName = remoteBranch.getName();
        this.headSha = remoteBranch.getSHA1();
        try {
            this.htmlUrl = new URL(remoteBranch.getOwner().getHtmlUrl().toString() + "/" + branchName);
        } catch (MalformedURLException ex) {
            LOG.error("Can't make URL for {}", remoteBranch, ex);
        }
    }

    public GitHubBranchCause(String reason, String branch, String headSha, boolean skip) {
        this.reason = reason;
        this.branchName = branch;
        this.headSha = headSha;
        this.skip = skip;
    }

    public GitHubBranchCause(GHBranch remoteBranch, String reason, boolean skip) {
        this.reason = reason;
        this.skip = skip;
        this.branchName = remoteBranch.getName();
        this.headSha = remoteBranch.getSHA1();
        try {
            this.htmlUrl = new URL(remoteBranch.getOwner().getHtmlUrl().toString() + "/" + branchName);
        } catch (MalformedURLException ex) {
            LOG.error("Can't make URL for {}", remoteBranch, ex);
        }
    }

    @Override
    public String getShortDescription() {
        return "GitHub Branch #<a href=\"" + htmlUrl + "\">" + branchName + "</a>, " + reason;
    }

    @Override
    public void onAddedTo(@Nonnull Run run) {
        // move polling log from cause to action
        try {
            GitHubBranchPollingLogAction action = new GitHubBranchPollingLogAction(run);
            writeStringToFile(action.getPollingLogFile(), pollingLog);
            run.replaceAction(action);
        } catch (IOException ex) {
            LOG.warn("Failed to persist the polling log", ex);
        }
        pollingLog = null;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getHeadSha() {
        return headSha;
    }
}
