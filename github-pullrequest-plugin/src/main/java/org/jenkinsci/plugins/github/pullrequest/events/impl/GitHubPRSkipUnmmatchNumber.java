package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;

/**
 * Skip PR checks when un-matched to PR number.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRSkipUnmatchNumber extends GitHubPREvent {
    private static final String DISPLAY_NAME = "PR Number";

    private Integer number;

    @DataBoundConstructor
    public GitHubPRSkipUnmatchNumber(Integer number) {
        this.number = number;
    }

    @CheckForNull
    public Integer getNumber() {
        return number;
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        if (number == null) {
            listener.error("Number is null -> Bad configured event, skipping other checks.");
            return new GitHubPRCause(remotePR, "Bad configured " + DISPLAY_NAME + " event.", true);
        }

        if (remotePR != null && remotePR.getNumber() == getNumber()) {
            return new GitHubPRCause(remotePR, "Building matched pr " + remotePR.getNumber(), false);
        }

        if (localPR != null && localPR.getNumber() == getNumber()) {
            return new GitHubPRCause(remotePR, "Building matched pr " + localPR.getNumber(), false);
        }

        return null;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
