package com.github.kostyasha.github.integration.generic;

import antlr.ANTLRException;
import hudson.model.Job;
import hudson.triggers.Trigger;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubTrigger<T extends GitHubTrigger<T>> extends Trigger<Job<?, ?>> {
    protected GitHubTrigger(String cronTabSpec) throws ANTLRException {
        super(cronTabSpec);
    }

    protected GitHubTrigger() {
    }
}
