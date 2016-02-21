package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trigger run when label exists. Remove label in post-build action to exclude cycle builds.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRLabelNotExistsEvent extends GitHubPREvent {
    private static final String DISPLAY_NAME = "Labels not exist";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRLabelNotExistsEvent.class);

    private final GitHubPRLabel label;
    private boolean skip = true;

    @DataBoundConstructor
    public GitHubPRLabelNotExistsEvent(GitHubPRLabel label, boolean skip) {
        this.label = label;
        this.skip = skip;
    }

    @Override
    public GitHubPRCause check(GitHubPRTrigger gitHubPRTrigger, GHPullRequest remotePR,
                               @CheckForNull GitHubPRPullRequest localPR, TaskListener listener) throws IOException {
        if (remotePR.getState().equals(GHIssueState.CLOSED)) {
            return null; // already closed, skip check?
        }

        GitHubPRCause cause = null;

        Collection<GHLabel> remoteLabels = remotePR.getRepository().getIssue(remotePR.getNumber()).getLabels();
        Set<String> existingLabels = new HashSet<String>();

        for (GHLabel ghLabel : remoteLabels) {
            existingLabels.add(ghLabel.getName());
        }

        existingLabels.retainAll(label.getLabelsSet());

        if (existingLabels.isEmpty()) {
            final PrintStream logger = listener.getLogger();
            logger.println(DISPLAY_NAME + ": " + label.getLabelsSet() + " not found");
            cause = new GitHubPRCause(remotePR, label.getLabelsSet() + " labels not exist", isSkip());
        }

        return cause;
    }

    public GitHubPRLabel getLabel() {
        return label;
    }

    public boolean isSkip() {
        return skip;
    }

    @Extension
    public static class DescriptorImpl extends GitHubPREventDescriptor {

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
