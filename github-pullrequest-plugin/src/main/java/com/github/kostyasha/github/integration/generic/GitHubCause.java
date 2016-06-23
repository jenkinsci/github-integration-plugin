package com.github.kostyasha.github.integration.generic;

import hudson.model.Cause;
import hudson.model.Run;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.apache.commons.io.FileUtils.readFileToString;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubCause<T extends GitHubCause<T>> extends Cause {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubCause.class);

    protected boolean skip;
    protected String reason;
    protected URL htmlUrl;

    protected String pollingLog;

    public boolean isSkip() {
        return skip;
    }

    /**
     * @see #skip
     */
    public GitHubCause<T> withSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    // for printing PR url on left builds panel (build description)
    public URL getHtmlUrl() {
        return htmlUrl;
    }

    /**
     * @see #htmlUrl
     */
    public GitHubCause<T> withHtmlUrl(URL htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    /**
     * @see #pollingLog
     */
    public GitHubCause<T> withPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
        return this;
    }

    public void setPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
    }

    public void setPollingLog(File logFile) throws IOException {
        this.pollingLog = readFileToString(logFile);
    }

    @Override
    public void onAddedTo(@Nonnull Run run) {
        // move polling log from cause to action
        try {
            GitHubPRPollingLogAction action = new GitHubPRPollingLogAction(run);
            FileUtils.writeStringToFile(action.getPollingLogFile(), pollingLog);
            run.replaceAction(action);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist the polling log", e);
        }
        pollingLog = null;
    }

    public String getReason() {
        return reason;
    }
}
