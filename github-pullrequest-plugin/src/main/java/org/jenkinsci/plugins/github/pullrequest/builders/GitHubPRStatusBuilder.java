package org.jenkinsci.plugins.github.pullrequest.builders;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRCauseFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.triggerFrom;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * Sets pr status for build caused by GitHubPRCause
 */
public class GitHubPRStatusBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRStatusBuilder.class);

    public static final GitHubPRMessage DEFAULT_MESSAGE = new GitHubPRMessage("${GITHUB_PR_COND_REF} run started");

    @CheckForNull
    private GitHubPRMessage statusMessage = DEFAULT_MESSAGE;

    /**
     * Constructor with defaults. Only for groovy UI.
     */
    @Restricted(NoExternalUse.class)
    public GitHubPRStatusBuilder() {
    }

    @DataBoundConstructor
    public GitHubPRStatusBuilder(GitHubPRMessage statusMessage) {
        if (statusMessage != null && isNotBlank(statusMessage.getContent())) {
            this.statusMessage = statusMessage;
        }
    }

    public GitHubPRMessage getStatusMessage() {
        return statusMessage;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws InterruptedException, IOException {
        // No triggers in Run class, but we need it
        final GitHubPRTrigger trigger = triggerFrom(run.getParent(), GitHubPRTrigger.class);
        if (isNull(trigger)) {
            // silently skip. TODO implement error handler, like in publishers
            return;
        }

        GitHubPRCause cause = ghPRCauseFromRun(run);
        if (isNull(cause)) {
            return;
        }

        // GitHub status for commit
        try {
            if (nonNull(statusMessage)) {
                String url = trigger.getDescriptor().getJenkinsURL() + run.getUrl();

                trigger.getRemoteRepo().createCommitStatus(
                        cause.getHeadSha(),
                        GHCommitState.PENDING,
                        url,
                        statusMessage.expandAll(run, listener),
                        run.getParent().getFullName()
                );
            }
        } catch (Exception e) {
            listener.getLogger().println("Can't update build description");
            LOGGER.error("Can't set commit status", e);
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // workflow wants Run, but API doesn't support it
            return true;
        }

        @Override
        public String getDisplayName() {
            return "GitHub PR: set 'pending' status";
        }
    }
}
