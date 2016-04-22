package org.jenkinsci.plugins.github.pullrequest;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.triggers.SCMTrigger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
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

    private final String headSha;
    private final int number;
    private final boolean mergeable;
    private final String targetBranch;
    private final String sourceBranch;
    private final String prAuthorEmail;
    @CheckForNull
    private final String title;
    private final URL htmlUrl;
    private final String sourceRepoOwner;
    private String triggerSenderName = "";
    private String triggerSenderEmail = "";
    private Set<String> labels;
    private final String reason;
    /**
     * In case triggered because of commit.
     * See {@link org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent}
     */
    private final String commitAuthorName;
    private final String commitAuthorEmail;

    private boolean skip;
    private String condRef;
    private String pollingLog;

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
                triggerSender, skip, reason, "", "");
    }

    //FIXME (sizes) ParameterNumber: More than 7 parameters (found 15).
    //CHECKSTYLE:OFF
    public GitHubPRCause(String headSha, int number, boolean mergeable,
                         String targetBranch, String sourceBranch, String prAuthorEmail,
                         String title, URL htmlUrl, String sourceRepoOwner, Set<String> labels,
                         GHUser triggerSender, boolean skip, String reason,
                         String commitAuthorName, String commitAuthorEmail) {
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

    //CHECKSTYLE:OFF
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubPRCause cause = (GitHubPRCause) o;

        if (number != cause.number) return false;
        if (mergeable != cause.mergeable) return false;
        if (skip != cause.skip) return false;
        if (headSha != null ? !headSha.equals(cause.headSha) : cause.headSha != null) return false;
        if (targetBranch != null ? !targetBranch.equals(cause.targetBranch) : cause.targetBranch != null) return false;
        if (sourceBranch != null ? !sourceBranch.equals(cause.sourceBranch) : cause.sourceBranch != null) return false;
        if (prAuthorEmail != null ? !prAuthorEmail.equals(cause.prAuthorEmail) : cause.prAuthorEmail != null)
            return false;
        if (title != null ? !title.equals(cause.title) : cause.title != null) return false;
        if (htmlUrl != null ? !htmlUrl.equals(cause.htmlUrl) : cause.htmlUrl != null) return false;
        if (sourceRepoOwner != null ? !sourceRepoOwner.equals(cause.sourceRepoOwner) : cause.sourceRepoOwner != null)
            return false;
        if (triggerSenderName != null ? !triggerSenderName.equals(cause.triggerSenderName) : cause.triggerSenderName != null)
            return false;
        if (triggerSenderEmail != null ? !triggerSenderEmail.equals(cause.triggerSenderEmail) : cause.triggerSenderEmail != null)
            return false;
        if (labels != null ? !labels.equals(cause.labels) : cause.labels != null) return false;
        if (reason != null ? !reason.equals(cause.reason) : cause.reason != null) return false;
        if (commitAuthorName != null ? !commitAuthorName.equals(cause.commitAuthorName) : cause.commitAuthorName != null)
            return false;
        if (commitAuthorEmail != null ? !commitAuthorEmail.equals(cause.commitAuthorEmail) : cause.commitAuthorEmail != null)
            return false;
        if (condRef != null ? !condRef.equals(cause.condRef) : cause.condRef != null) return false;
        return !(pollingLog != null ? !pollingLog.equals(cause.pollingLog) : cause.pollingLog != null);

    }

    @Override
    public int hashCode() {
        int result = headSha != null ? headSha.hashCode() : 0;
        result = 31 * result + number;
        result = 31 * result + (mergeable ? 1 : 0);
        result = 31 * result + (targetBranch != null ? targetBranch.hashCode() : 0);
        result = 31 * result + (sourceBranch != null ? sourceBranch.hashCode() : 0);
        result = 31 * result + (prAuthorEmail != null ? prAuthorEmail.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (htmlUrl != null ? htmlUrl.hashCode() : 0);
        result = 31 * result + (sourceRepoOwner != null ? sourceRepoOwner.hashCode() : 0);
        result = 31 * result + (triggerSenderName != null ? triggerSenderName.hashCode() : 0);
        result = 31 * result + (triggerSenderEmail != null ? triggerSenderEmail.hashCode() : 0);
        result = 31 * result + (labels != null ? labels.hashCode() : 0);
        result = 31 * result + (reason != null ? reason.hashCode() : 0);
        result = 31 * result + (commitAuthorName != null ? commitAuthorName.hashCode() : 0);
        result = 31 * result + (commitAuthorEmail != null ? commitAuthorEmail.hashCode() : 0);
        result = 31 * result + (skip ? 1 : 0);
        result = 31 * result + (condRef != null ? condRef.hashCode() : 0);
        result = 31 * result + (pollingLog != null ? pollingLog.hashCode() : 0);
        return result;
    }
    //CHECKSTYLE:ON

    @Override
    public String toString() {
        return "GitHubPRCause{" +
                "headSha='" + headSha + '\'' +
                ", number=" + number +
                ", mergeable=" + mergeable +
                ", targetBranch='" + targetBranch + '\'' +
                ", sourceBranch='" + sourceBranch + '\'' +
                ", prAuthorEmail='" + prAuthorEmail + '\'' +
                ", title='" + title + '\'' +
                ", htmlUrl=" + htmlUrl +
                ", sourceRepoOwner='" + sourceRepoOwner + '\'' +
                ", triggerSenderName='" + triggerSenderName + '\'' +
                ", triggerSenderEmail='" + triggerSenderEmail + '\'' +
                ", labels=" + labels +
                ", reason='" + reason + '\'' +
                ", commitAuthorName='" + commitAuthorName + '\'' +
                ", commitAuthorEmail='" + commitAuthorEmail + '\'' +
                ", skip=" + skip +
                ", condRef='" + condRef + '\'' +
                ", pollingLog='" + pollingLog + '\'' +
                '}';
    }
}
