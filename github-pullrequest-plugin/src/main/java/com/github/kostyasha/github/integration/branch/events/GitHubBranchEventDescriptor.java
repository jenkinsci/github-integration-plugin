package com.github.kostyasha.github.integration.branch.events;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubBranchEventDescriptor extends Descriptor<GitHubBranchEvent> {
    public static DescriptorExtensionList<GitHubBranchEvent, GitHubBranchEventDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(GitHubBranchEvent.class);
    }
}
