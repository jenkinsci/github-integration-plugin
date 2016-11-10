package com.github.kostyasha.github.integration.branch.events;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

public abstract class GitHubBranchCommitEventDescriptor extends Descriptor<GitHubBranchCommitEvent> {
    public static DescriptorExtensionList<GitHubBranchCommitEvent, GitHubBranchCommitEventDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubBranchCommitEvent.class);
    }
}
