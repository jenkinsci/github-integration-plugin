package org.jenkinsci.plugins.github.pullrequest.pipeline;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.pullrequest.pipeline.SetCommitStatusStep.DescriptorImpl.FUNC_NAME;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

import hudson.AbortException;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.github.GHRepository;

import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.inject.Inject;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.JenkinsLocationConfiguration;

import java.io.IOException;

/**
 * Pipeline DSL step to update a GitHub commit status for a pull request.
 */
public class SetCommitStatusExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    private static final long serialVersionUID = 1L;

    @StepContextParameter
    private transient Run<?, ?> run;

    @StepContextParameter
    private transient TaskListener log;

    @Inject
    private transient SetCommitStatusStep config;

    @Override
    protected Void run() throws Exception {
        // Figure out which GitHub repository to send the request to
        checkArgument(nonNull(config.getState()), "Missing required parameter 'state'");

        GHRepository repository = resolveRepository();
        String statusContext = resolveContext();

        final GitHubPRCause cause = run.getCause(GitHubPRCause.class);
        if (isNull(cause)) {
            throw new AbortException(FUNC_NAME + " requires run to be triggered by GitHub Pull Request");
        }

        // Update the commit status
        log.getLogger().printf("Setting pull request status %s to %s with message: %s%n",
                statusContext,
                config.getState(),
                config.getMessage()
        );
        String buildUrl = null;
        final JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        if (nonNull(globalConfig)) {
            buildUrl = globalConfig.getUrl();
        }
        if (isEmpty(buildUrl)) {
            log.error("Jenkins Location is not configured in system settings. Cannot create a 'details' link.");
            buildUrl = null;
        } else {
            buildUrl += run.getUrl();
        }

        repository.createCommitStatus(cause.getHeadSha(),
                config.getState(),
                buildUrl,
                config.getMessage(),
                statusContext);
        return null;
    }

    /**
     * Looks up the GitHub repository associated with this build. This expects that the Pull Request
     * Trigger is configured properly for this project and that it specifies a GitHub project which
     * has a corresponding "Server" configuration in the global settings (which provides the
     * necessary credentials).
     */
    private GHRepository resolveRepository() throws IOException {
        // The trigger already has the ability to look this up, D.R.Y.
        try {
            GitHubPRTrigger trigger = JobInfoHelpers.triggerFrom(run.getParent(), GitHubPRTrigger.class);
            if (trigger != null) {
                return trigger.getRemoteRepo();
            } else {
                throw new AbortException("GitHub PullRequest trigger isn't available.");
            }
        } catch (Exception e) {
            log.error("pullRequest: GitHub repository not configured for project?");
            throw e;
        }
    }

    /**
     * Determines the commit status "context" to use for the status update. The context appears in
     * GitHub as a sort of name for the check. It identifies a status check among the other status
     * checks placed on the same commit.
     * <p>
     * The context name is specified as an argument to this step function. If that argument is
     * omitted, however, it will fall back to the "Display Name" property of the GitHub project
     * property of the build. Lastly, if that is also omitted, it will simply use the id/name of
     * the project as the context name.
     */
    private String resolveContext() {
        if (isNotBlank(config.getContext())) {
            return config.getContext();
        }
        GithubProjectProperty githubProperty = run.getParent().getProperty(GithubProjectProperty.class);
        if (isNull(githubProperty) || isBlank(githubProperty.getDisplayName())) {
            log.error("Unable to determine commit status context (the check name). "
                    + "Argument 'context' not provided and no default configured. Using job name as fallback.");
            return run.getParent().getFullName();
        }
        return githubProperty.getDisplayName();
    }
}
