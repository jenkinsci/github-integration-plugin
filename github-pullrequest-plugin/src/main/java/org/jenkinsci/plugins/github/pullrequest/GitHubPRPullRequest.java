package org.jenkinsci.plugins.github.pullrequest;

import hudson.Functions;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * Maintains state about a Pull Request for a particular Jenkins job.  This is what understands the current state
 * of a PR for a particular job. Instances of this class are immutable.
 * Used from {@link GitHubPRRepository}
 */
public class GitHubPRPullRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRPullRequest.class);

    private final int number;
    // https://github.com/kohsuke/github-api/issues/178
    private final Date issueUpdatedAt;
    private String title;
    private Date prUpdatedAt;
    private String headSha;
    private String headRef;
    private Boolean mergeable;
    private String baseRef;
    private String userEmail;
    private String userLogin;
    private URL htmlUrl;
    private Set<String> labels;
    @CheckForNull
    private Date lastCommentCreatedAt;
    private String sourceRepoOwner;
    private String state;

    /**
     * Save only what we need for next comparison
     */
    public GitHubPRPullRequest(GHPullRequest pr) throws IOException {
        userLogin = pr.getUser().getLogin();
        number = pr.getNumber();
        prUpdatedAt = pr.getUpdatedAt();
        issueUpdatedAt = pr.getIssueUpdatedAt();
        headSha = pr.getHead().getSha();
        headRef = pr.getHead().getRef();
        title = pr.getTitle();
        baseRef = pr.getBase().getRef();
        htmlUrl = pr.getHtmlUrl();

        try {
            Date maxDate = new Date(0);
            for (GHIssueComment comment : pr.getComments()) {
                if (comment.getCreatedAt().compareTo(maxDate) > 0) {
                    maxDate = comment.getCreatedAt();
                }
            }
            lastCommentCreatedAt = maxDate.getTime() == 0 ? null : new Date(maxDate.getTime());
        } catch (IOException e) {
            LOGGER.warn("Can't get comments for PR: {}", e.getMessage());
            lastCommentCreatedAt = null;
        }

        try {
            userEmail = pr.getUser().getEmail();
        } catch (Exception e) {
            LOGGER.warn("Can't get GitHub user email: {}", e.getMessage());
            userEmail = "";
        }

        GHRepository remoteRepo = pr.getRepository();

        try {
            updateLabels(remoteRepo.getIssue(number).getLabels());
        } catch (IOException e) {
            LOGGER.warn("Can't retrieve label list: {}", e.getMessage());
        }

        // see https://github.com/kohsuke/github-api/issues/111
        try {
            mergeable = pr.getMergeable();
        } catch (IOException e) {
            LOGGER.warn("Can't get mergeable status: {}", e.getMessage());
            mergeable = false;
        }

        sourceRepoOwner = remoteRepo.getOwnerName();
        state = pr.getState().toString();
    }

    public int getNumber() {
        return number;
    }

    public String getHeadSha() {
        return headSha;
    }

    public boolean isMergeable() {
        return isNull(mergeable) ? false : mergeable;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public String getHeadRef() {
        return headRef;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getTitle() {
        return title;
    }

    public Set<String> getLabels() {
        return labels;
    }

    @CheckForNull
    public Date getLastCommentCreatedAt() {
        return lastCommentCreatedAt;
    }

    /**
     * URL to the Github Pull Request.
     */
    public URL getHtmlUrl() {
        return htmlUrl;
    }

    public Date getPrUpdatedAt() {
        return new Date(prUpdatedAt.getTime());
    }

    public Date getIssueUpdatedAt() {
        return issueUpdatedAt;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getSourceRepoOwner() {
        return sourceRepoOwner;
    }

    /**
     * @see #state
     */
    @CheckForNull
    public String getState() {
        return state;
    }

    private void updateLabels(Collection<GHLabel> labels) {
        this.labels = new HashSet<>();
        for (GHLabel label : labels) {
            this.labels.add(label.getName());
        }
    }

    public static String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
