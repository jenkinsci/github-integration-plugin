package org.jenkinsci.plugins.github.pullrequest.publishers.impl;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Api;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCheckName;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.publishers.GitHubPRAbstractPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * Sets build status on GitHub.
 *
 * @author Alina Karpovich
 * @author Kanstantsin Shautsou
 */
public class GitHubPRBuildStatusPublisher extends GitHubPRAbstractPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRBuildStatusPublisher.class);

    private GitHubPRMessage statusMsg = new GitHubPRMessage("${GITHUB_PR_COND_REF} run ended");
    private GHCommitState unstableAs = GHCommitState.FAILURE;
    private BuildMessage buildMessage = new BuildMessage();
    private GitHubPRCheckName customCheck;
    private CommitState forceStatus;

    /**
     * Constructor with defaults. Only for groovy UI.
     */
    @Restricted(NoExternalUse.class)
    public GitHubPRBuildStatusPublisher() {
        super(null, null);
    }

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

    @DataBoundSetter
    public void setCustomCheck(GitHubPRCheckName customCheck) {
        this.customCheck = customCheck;
    }

    @DataBoundSetter
    public void setForceStatus(CommitState state) {
        this.forceStatus = state;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream listenerLogger = listener.getLogger();
        String publishedURL = getTriggerDescriptor().getJenkinsURL();

        if (getStatusVerifier() != null && !getStatusVerifier().isRunAllowed(run)) {
            return;
        }

        if (isEmpty(publishedURL)) {
            return;
        }

        GHCommitState state = null == forceStatus ? getCommitState(run, unstableAs) : forceStatus.getState();

        GitHubPRCause c = run.getCause(GitHubPRCause.class);

        String statusMsgValue = getStatusMsg().expandAll(run, listener);
        String buildUrl = publishedURL + run.getUrl();

        LOGGER.info("Setting status of {} to {} with url {} and message: {}",
                c.getHeadSha(), state, buildUrl, statusMsgValue);

        // TODO check permissions to write human friendly message
        final GitHubPRTrigger trigger = JobInfoHelpers.triggerFrom(run.getParent(), GitHubPRTrigger.class);
        if (isNull(trigger)) {
            // silently skip. TODO implement error handler, like in publishers
            return;
        }

        String context = run.getParent().getFullName();
        if (null != customCheck) {
            context = defaultIfBlank(customCheck.getCheckName(), context);
        }

        try {
            trigger.getRemoteRepo().createCommitStatus(c.getHeadSha(), state, buildUrl, statusMsgValue,
                    context);
        } catch (IOException ex) {
            if (nonNull(buildMessage)) {
                String comment = null;
                LOGGER.error("Could not update commit status of the Pull Request on GitHub. ", ex);
                if (state == GHCommitState.SUCCESS) {
                    comment = buildMessage.getSuccessMsg().expandAll(run, listener);
                } else if (state == GHCommitState.FAILURE) {
                    comment = buildMessage.getFailureMsg().expandAll(run, listener);
                }
                listenerLogger.println("Adding comment...");
                LOGGER.info("Adding comment, because: ", ex);
                addComment(c.getNumber(), comment, run, listener);
            } else {
                listenerLogger.println("Could not update commit status of the Pull Request on GitHub." + ex.getMessage());
                LOGGER.error("Could not update commit status of the Pull Request on GitHub.", ex);
            }
            handlePublisherError(run);
        }
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

    public GitHubPRCheckName getCustomCheck() {
        return customCheck;
    }

    public CommitState getForceStatus() {
        return forceStatus;
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

    public static class CommitState extends AbstractDescribableImpl<CommitState> {
        private GHCommitState state;

        @DataBoundConstructor
        public CommitState(GHCommitState state) {
            this.state = state;
        }

        public CommitState() {

        }
        public GHCommitState getState() {
            return state;
        }
        public void setState(GHCommitState state) {
            this.state = state;
        }

        @Override
        public Descriptor<CommitState> getDescriptor() {
            return super.getDescriptor();
        }
        @Extension
        public static class DescriptorImpl extends Descriptor<CommitState> {
            @Override
            public String getDisplayName() {
                return "Commit status container";
            }
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
