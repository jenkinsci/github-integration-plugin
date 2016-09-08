package com.github.kostyasha.github.integration.generic;

import hudson.model.Cause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubCause<T extends GitHubCause<T>> extends Cause {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubCause.class);

    private boolean skip;
    private String reason;

    /**
     * Doesn't exist for deleted branch.
     */
    @CheckForNull
    private URL htmlUrl;

    @CheckForNull
    protected String title;

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

    // for printing branch url on left builds panel (build description)
    @CheckForNull
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

    public String getReason() {
        return reason;
    }

    /**
     * @see #reason
     */
    public GitHubCause<T> withReason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * Returns the title of the cause, never null.
     */
    @Nonnull
    public String getTitle() {
        return nonNull(title) ? title : "";
    }

    /**
     * Returns at most the first 30 characters of the title, or
     */
    public String getAbbreviatedTitle() {
        return abbreviate(getTitle(), 30);
    }
}
