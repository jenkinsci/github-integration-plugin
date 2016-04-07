package org.jenkinsci.plugins.github.pullrequest.pipeline;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.github.GHRepository;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.inject.Inject;

import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.JenkinsLocationConfiguration;

/**
 * Pipeline DSL step to update a GitHub commit status for a pull request.
 */
public class SetCommitStatusExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

    /** YYYYMMDD */
    private static final long serialVersionUID = 20160401L;

    @StepContextParameter
    private transient Run<?, ?> build;

    @StepContextParameter
    private transient TaskListener log;

    @Inject
    private transient SetCommitStatusStep config;

    @Override
    protected Void run() throws Exception {
        //
        // Figure out which GitHub repository to send the request to
        //
        checkArgument(nonNull(config.getState()), "Missing required parameter 'state'");

        GHRepository repository = resolveRepository();
        String statusContext = resolveContext();

        final GitHubPRCause cause = build.getCause(GitHubPRCause.class);
        if (isNull(cause)) {
            throw new ProjectConfigurationException("pullRequestSetCommitStatus requires build to be triggered by GitHub Pull Request");
        }
        //
        // Update the commit status
        //
        log.getLogger().printf("Setting pull request status %s to %s with message: %s%n",
                               statusContext,
                               config.getState(),
                               config.getMessage());
        String buildUrl = null;
        final JenkinsLocationConfiguration globalConfig = JenkinsLocationConfiguration.get();
        if (nonNull(globalConfig)) {
            buildUrl = globalConfig.getUrl();
        }
        if (isEmpty(buildUrl)) {
            log.error("Jenkins Location is not configured in system settings. Cannot create a 'details' link.");
            buildUrl = null;
        } else {
            buildUrl += build.getUrl();
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
    private GHRepository resolveRepository() throws ProjectConfigurationException {
        String repositoryURL;
        //
        // Use the GitHub repo as defined in the job settings
        //
        GithubProjectProperty githubProperty = build.getParent().getProperty(GithubProjectProperty.class);
        if (isNull(githubProperty) || isBlank(githubProperty.getProjectUrlStr())) {
            throw new ProjectConfigurationException("pullRequest: GitHub repository not configured for build project");
        }
        repositoryURL = githubProperty.getProjectUrlStr();

        GitHubRepositoryName repoName = GitHubRepositoryName.create(repositoryURL);
        if (isNull(repoName)) {
            throw new ProjectConfigurationException("Invalid URL \"" + repositoryURL + "\" provided as GitHub project");
        }

        GHRepository resolvedRepo = repoName.resolveOne();
        if (isNull(resolvedRepo)) {
            throw new ProjectConfigurationException("No configuration found for GitHub server at \""
                                                            + repositoryURL + "\". Check global configuration.");
        }
        return resolvedRepo;
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
        GithubProjectProperty githubProperty = build.getParent().getProperty(GithubProjectProperty.class);
        if (isNull(githubProperty) || isBlank(githubProperty.getDisplayName())) {
            log.error("Unable to determine commit status context (the check name). "
                           + "Argument 'context' not provided and no default configured. Using job name as fallback.");
            return build.getParent().getFullName();
        }
        return githubProperty.getDisplayName();
    }
}
