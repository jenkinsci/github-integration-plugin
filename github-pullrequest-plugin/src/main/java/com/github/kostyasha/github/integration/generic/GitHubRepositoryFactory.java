package com.github.kostyasha.github.integration.generic;

import hudson.XmlFile;
import hudson.model.Job;
import hudson.util.FormValidation;
import jenkins.model.TransientActionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
