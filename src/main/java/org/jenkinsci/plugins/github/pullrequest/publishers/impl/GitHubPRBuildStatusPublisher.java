package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Api;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Sets build status on GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBuildStatusPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRBuildStatusPublisher.class);

    private GitHubPRMessage statusMsg = new GitHubPRMessage("$GITHUB_PR_COND_REF run ended");
    private GHCommitState unstableAs = GHCommitState.FAILURE;
    private BuildMessage buildMessage = new BuildMessage();

    @DataBoundConstructor
    public GitHubPRBuildStatusPublisher(GitHubPRMessage statusMsg, GHCommitState unstableAs, BuildMessage buildMessage,
                                        StatusVerifier statusVerifier, PublisherErrorHandler errorHandler) {
        super(statusVerifier, errorHandler);
        if (statusMsg != null && isNotEmpty(statusMsg.getContent())) {
            this.statusMsg = statusMsg;
        }
        this.unstableAs = unstableAs;
        this.buildMessage = buildMessage;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws
            InterruptedException, IOException {
        PrintStream listenerLogger = listener.getLogger();
        String publishedURL = getTriggerDescriptor().getJenkinsURL();

        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(build)) {
            return true;
        }

        if (publishedURL != null && !publishedURL.isEmpty()) {
            GHCommitState state = getCommitState(build, unstableAs);

            GitHubPRCause c = build.getCause(GitHubPRCause.class);

            String statusMsgValue = getStatusMsg().expandAll(build, listener);
            String buildUrl = publishedURL + build.getUrl();

            LOGGER.info("Setting status of {} to {} with url {} and message: {}",
                    c.getHeadSha(), state, buildUrl, statusMsgValue);

            try {
                // TODO check permissions to write human friendly message
                build.getProject()
                        .getTrigger(GitHubPRTrigger.class)
                        .getRemoteRepo()
                        .createCommitStatus(c.getHeadSha(), state, buildUrl, statusMsgValue, build.getProject().getFullName());
            } catch (IOException ex) {
                if (buildMessage != null) {
                    String comment = null;
                    LOGGER.error("Could not update commit status of the Pull Request on GitHub. ", ex);
                    if (state == GHCommitState.SUCCESS) {
                        comment = buildMessage.getSuccessMsg().expandAll(build, listener);
                    } else if (state == GHCommitState.FAILURE) {
                        comment = buildMessage.getFailureMsg().expandAll(build, listener);
                    }
                    listenerLogger.println("Adding comment...");
                    LOGGER.info("Adding comment, because: ", ex);
                    addComment(c.getNumber(), comment, build, listener);
                } else {
                    listenerLogger.println("Could not update commit status of the Pull Request on GitHub." + ex.getMessage());
                    LOGGER.error("Could not update commit status of the Pull Request on GitHub.", ex);
                }
                handlePublisherError(build);
            }
        }
        return true;
    }

    public final Api getApi() {
        return new Api(this);
    }

    public BuildMessage getBuildMessage() {
        return buildMessage;
    }

    public GHCommitState getUnstableAs() {
        return unstableAs;
    }

    public GitHubPRMessage getStatusMsg() {
        return statusMsg;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "GitHub PR: set PR status";
        }
    }

    public static class BuildMessage extends AbstractDescribableImpl<BuildMessage> {
        private GitHubPRMessage successMsg = new GitHubPRMessage("Can't set status; build succeeded.");
        private GitHubPRMessage failureMsg = new GitHubPRMessage("Can't set status; build failed.");

        @DataBoundConstructor
        public BuildMessage(GitHubPRMessage successMsg, GitHubPRMessage failureMsg) {
            this.successMsg = successMsg;
            this.failureMsg = failureMsg;
        }

        public BuildMessage() {
        }

        public GitHubPRMessage getSuccessMsg() {
            return successMsg;
        }

        public void setSuccessMsg(GitHubPRMessage successMsg) {
            this.successMsg = successMsg;
        }

        public GitHubPRMessage getFailureMsg() {
            return failureMsg;
        }

        public void setFailureMsg(GitHubPRMessage failureMsg) {
            this.failureMsg = failureMsg;
        }

        @Override
        public DescriptorImpl getDescriptor() {
            return (DescriptorImpl) super.getDescriptor();
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<BuildMessage> {
            @Override
            public String getDisplayName() {
                return "Build message container";
            }
        }
    }
}
