package com.github.kostyasha.github.integration.generic;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.Job;
import hudson.triggers.Trigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubTrigger<T extends GitHubTrigger<T>> extends Trigger<Job<?, ?>> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubTrigger.class);

    @CheckForNull
    protected GitHubPRTriggerMode triggerMode = CRON;

    /**
     * Cancel queued runs for specific kind (i.e. PR by number, branch by name).
     */
    protected boolean cancelQueued = false;
    protected boolean abortRunning = false;
    protected boolean skipFirstRun = false;

    // for performance
    protected transient GitHubRepositoryName repoName;
    protected transient GHRepository remoteRepository;

    protected GitHubTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    public GitHubTrigger(String spec, GitHubPRTriggerMode triggerMode) throws ANTLRException {
        super(spec);
        this.triggerMode = triggerMode;
    }

    public abstract String getFinishMsg();

    @CheckForNull
    public Job<?, ?> getJob() {
        return job;
    }

    public GitHubRepositoryName getRepoFullName(Job<?, ?> job) {
        if (isNull(repoName)) {
            checkNotNull(job, "job object is null, race condition?");
            GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);

            checkNotNull(ghpp, "GitHub project property is not defined. Can't setup GitHub PR trigger for job %s",
                    job.getName());
            checkNotNull(ghpp.getProjectUrl(), "A GitHub project url is required");

            GitHubRepositoryName repo = GitHubRepositoryName.create(ghpp.getProjectUrl().baseUrl());

            checkNotNull(repo, "Invalid GitHub project url: %s", ghpp.getProjectUrl().baseUrl());

            repoName = repo;
        }

        return repoName;
    }

    public void trySave() {
        try {
            job.save();
        } catch (IOException e) {
            LOG.error("Error while saving job to file", e);
        }
    }

    protected void saveIfSkipFirstRun() {
        if (skipFirstRun) {
            LOG.info("Skipping first run for {}", job.getFullName());
            skipFirstRun = false;
            trySave();
        }
    }
}
