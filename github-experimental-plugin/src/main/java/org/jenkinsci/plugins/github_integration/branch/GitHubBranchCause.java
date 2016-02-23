package org.jenkinsci.plugins.github_integration.branch;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import org.apache.commons.io.FileUtils;
import org.kohsuke.github.GHBranch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCause extends Cause {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchCause.class);

    private String pollingLog;

    private String reason;
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
    public void onAddedTo(@Nonnull Run build) {
        try {
            SCMTrigger.BuildAction action = new SCMTrigger.BuildAction(build);
            FileUtils.writeStringToFile(action.getPollingLogFile(), pollingLog);
            build.replaceAction(action);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist the polling log", e);
        }
        pollingLog = null;
    }

    @Override
    public String getShortDescription() {
        return "Short description";
    }

    public String getReason() {
        return reason;
    }

    public void setPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
    }

    public void setPollingLog(File logFile) throws IOException {
        this.pollingLog = FileUtils.readFileToString(logFile);
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getHeadSha() {
        return headSha;
    }
}
