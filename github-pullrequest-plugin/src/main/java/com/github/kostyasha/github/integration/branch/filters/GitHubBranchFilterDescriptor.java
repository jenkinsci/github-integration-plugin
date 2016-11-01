package com.github.kostyasha.github.integration.branch.filters;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

public abstract class GitHubBranchFilterDescriptor extends Descriptor<GitHubBranchFilter> {
    public static DescriptorExtensionList<GitHubBranchFilter, GitHubBranchFilterDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubBranchFilter.class);
    }
}
