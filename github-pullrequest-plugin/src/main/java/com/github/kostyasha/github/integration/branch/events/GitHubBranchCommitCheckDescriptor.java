package com.github.kostyasha.github.integration.branch.events;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

public abstract class GitHubBranchCommitCheckDescriptor extends Descriptor<GitHubBranchCommitCheck> {
    public static DescriptorExtensionList<GitHubBranchCommitCheck, GitHubBranchCommitCheckDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubBranchCommitCheck.class);
    }
}
