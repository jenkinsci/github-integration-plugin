package org.jenkinsci.plugins.github.pullrequest;

import hudson.model.Cause;
import hudson.model.Run;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

public class GitHubPRCause extends Cause {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRCause.class);

    private String headSha;
    private int number;
    private boolean mergeable;
    private String targetBranch;
    private String sourceBranch;
    private String prAuthorEmail;
    @CheckForNull
    private String title;
    private URL htmlUrl;
    private String sourceRepoOwner;
    private String triggerSenderName = "";
    private String triggerSenderEmail = "";
    private Set<String> labels;
    private String reason;
    /**
     * In case triggered because of commit.
     * See {@link org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent}
     */
    private String commitAuthorName;
    private String commitAuthorEmail;

    private boolean skip;
    private String condRef;
    private String pollingLog;
    private String state;

    public GitHubPRCause() {
    }

    public GitHubPRCause(GHPullRequest remotePr,
                         String reason,
                         boolean skip) throws IOException {
        this(new GitHubPRPullRequest(remotePr), remotePr.getUser(), skip, reason);
    }

    public GitHubPRCause(GitHubPRPullRequest pr,
                         GHUser triggerSender,
                         boolean skip,
                         String reason) throws IOException {
        this(pr.getHeadSha(), pr.getNumber(),
                pr.isMergeable(), pr.getBaseRef(), pr.getHeadRef(),
                pr.getUserEmail(), pr.getTitle(), pr.getHtmlUrl(), pr.getSourceRepoOwner(),
                pr.getLabels(),
                triggerSender, skip, reason, "", "", pr.getState());
    }

    //FIXME (sizes) ParameterNumber: More than 7 parameters (found 15).
    //CHECKSTYLE:OFF
    public GitHubPRCause(String headSha, int number, boolean mergeable,
                         String targetBranch, String sourceBranch, String prAuthorEmail,
                         String title, URL htmlUrl, String sourceRepoOwner, Set<String> labels,
                         GHUser triggerSender, boolean skip, String reason,
                         String commitAuthorName, String commitAuthorEmail,
                         String state) {
    //CHECKSTYLE:ON
        this.headSha = headSha;
        this.number = number;
        this.mergeable = mergeable;
        this.targetBranch = targetBranch;
        this.sourceBranch = sourceBranch;
        this.prAuthorEmail = prAuthorEmail;
        this.title = title;
        this.htmlUrl = htmlUrl;
        this.sourceRepoOwner = sourceRepoOwner;
        this.labels = labels;
        this.skip = skip;
        this.reason = reason;
        this.commitAuthorName = commitAuthorName;
        this.commitAuthorEmail = commitAuthorEmail;

        if (nonNull(triggerSender)) {
            try {
                this.triggerSenderName = triggerSender.getName();
            } catch (IOException e) {
                LOGGER.error("Can't get trigger sender name from remote PR");
            }

            try {
                this.triggerSenderEmail = triggerSender.getEmail();
            } catch (IOException e) {
                LOGGER.error("Can't get trigger sender email from remote PR");
            }
        }

        this.condRef = mergeable ? "merge" : "head";

        this.state = state;
    }

    public static GitHubPRCause newGitHubPRCause() {
        return new GitHubPRCause();
    }

    /**
     * @see #headSha
     */
    public GitHubPRCause withHeadSha(String headSha) {
        this.headSha = headSha;
        return this;
    }

    /**
     * @see #number
     */
    public GitHubPRCause withNumber(int number) {
        this.number = number;
        return this;
    }

    /**
     * @see #mergeable
     */
    public GitHubPRCause withMergeable(boolean mergeable) {
        this.mergeable = mergeable;
        return this;
    }

    /**
     * @see #targetBranch
     */
    public GitHubPRCause withTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
        return this;
    }

    /**
     * @see #sourceBranch
     */
    public GitHubPRCause withSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
        return this;
    }

    /**
     * @see #prAuthorEmail
     */
    public GitHubPRCause withPrAuthorEmail(String prAuthorEmail) {
        this.prAuthorEmail = prAuthorEmail;
        return this;
    }

    /**
     * @see #title
     */
    public GitHubPRCause withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * @see #htmlUrl
     */
    public GitHubPRCause withHtmlUrl(URL htmlUrl) {
        this.htmlUrl = htmlUrl;
        return this;
    }

    /**
     * @see #sourceRepoOwner
     */
    public GitHubPRCause withSourceRepoOwner(String sourceRepoOwner) {
        this.sourceRepoOwner = sourceRepoOwner;
        return this;
    }

    /**
     * @see #triggerSenderName
     */
    public GitHubPRCause withTriggerSenderName(String triggerSenderName) {
        this.triggerSenderName = triggerSenderName;
        return this;
    }

    /**
     * @see #triggerSenderEmail
     */
    public GitHubPRCause withTriggerSenderEmail(String triggerSenderEmail) {
        this.triggerSenderEmail = triggerSenderEmail;
        return this;
    }

    /**
     * @see #labels
     */
    public GitHubPRCause withLabels(Set<String> labels) {
        this.labels = labels;
        return this;
    }

    /**
     * @see #reason
     */
    public GitHubPRCause withReason(String reason) {
        this.reason = reason;
        return this;
    }

    /**
     * @see #commitAuthorName
     */
    public GitHubPRCause withCommitAuthorName(String commitAuthorName) {
        this.commitAuthorName = commitAuthorName;
        return this;
    }

    /**
     * @see #commitAuthorEmail
     */
    public GitHubPRCause withCommitAuthorEmail(String commitAuthorEmail) {
        this.commitAuthorEmail = commitAuthorEmail;
        return this;
    }
    /**
     * @see #state
     */
    public GitHubPRCause withState(String state) {
        this.state = state;
        return this;
    }

    /**
     * @see #skip
     */
    public GitHubPRCause withSkip(boolean skip) {
        this.skip = skip;
        return this;
    }

    /**
     * @see #condRef
     */
    public GitHubPRCause withCondRef(String condRef) {
        this.condRef = condRef;
        return this;
    }

    /**
     * @see #pollingLog
     */
    public GitHubPRCause withPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
        return this;
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

    @Override
    public String getShortDescription() {
        return "GitHub PR #<a href=\"" + htmlUrl + "\">" + number + "</a>, " + reason;
    }

    public String getHeadSha() {
        return headSha;
    }

    public boolean isMergeable() {
        return mergeable;
    }

    public int getNumber() {
        return number;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getPRAuthorEmail() {
        return prAuthorEmail;
    }

    // for printing PR url on left builds panel (build description)
    public URL getHtmlUrl() {
        return htmlUrl;
    }

    public String getSourceRepoOwner() {
        return sourceRepoOwner;
    }

    @Nonnull
    public Set<String> getLabels() {
        return isNull(labels) ? Collections.<String>emptySet() : labels;
    }

    public String getTriggerSenderName() {
        return triggerSenderName;
    }

    public String getTriggerSenderEmail() {
        return triggerSenderEmail;
    }

    public boolean isSkip() {
        return skip;
    }

    public String getReason() {
        return reason;
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
        return StringUtils.abbreviate(getTitle(), 30);
    }

    public String getPrAuthorEmail() {
        return prAuthorEmail;
    }

    public String getCommitAuthorName() {
        return commitAuthorName;
    }

    public String getCommitAuthorEmail() {
        return commitAuthorEmail;
    }

    public String getState() {
        return state;
    }

    @Nonnull
    public String getCondRef() {
        return condRef;
    }

    public void setPollingLog(String pollingLog) {
        this.pollingLog = pollingLog;
    }

    public void setPollingLog(File logFile) throws IOException {
        this.pollingLog = FileUtils.readFileToString(logFile);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
