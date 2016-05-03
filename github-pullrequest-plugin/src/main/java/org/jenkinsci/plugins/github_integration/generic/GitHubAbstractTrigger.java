package org.jenkinsci.plugins.github_integration.generic;

import antlr.ANTLRException;
import hudson.model.Job;
import hudson.triggers.Trigger;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubAbstractTrigger extends Trigger<Job<?, ?>> {
    public GitHubAbstractTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    public GitHubAbstractTrigger() {
    }
}
