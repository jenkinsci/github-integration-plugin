package com.github.kostyasha.github.integration.branch;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCause extends GitHubCause {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchCause.class);

    private final String sourceBranch;
    private final String headSha;


    public GitHubBranchCause(String reason, String pollingLog, String sourceBranch, String headSha) {
        this.reason = reason;
        this.pollingLog = pollingLog;
        this.sourceBranch = sourceBranch;
        this.headSha = headSha;
    }

    public GitHubBranchCause(GHBranch remoteBranch) {
        this.sourceBranch = remoteBranch.getName();
        this.headSha = remoteBranch.getSHA1();
    }

    @Override
    public String getShortDescription() {
        return "Short description";
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getHeadSha() {
        return headSha;
    }
}
