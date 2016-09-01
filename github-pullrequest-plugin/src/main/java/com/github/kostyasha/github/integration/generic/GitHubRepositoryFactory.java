package com.github.kostyasha.github.integration.generic;

import hudson.model.Job;
import jenkins.model.TransientActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepositoryFactory<R extends GitHubRepositoryFactory<R, T>, T extends GitHubTrigger<T>>
        extends TransientActionFactory<Job> {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepositoryFactory.class);

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
