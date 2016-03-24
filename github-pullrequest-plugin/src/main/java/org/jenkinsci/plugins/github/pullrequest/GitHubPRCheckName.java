package org.jenkinsci.plugins.github.pullrequest;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * Represents the name of a check on a PR. In the GitHub API, this is referred to as the "context"
 * of a commit status. This is the identifier used by GitHub to identify this type of check across
 * different pull requests. For example, this could be "unit-tests" to indicate that the commit
 * status refers to the outcome of running the unit tests against a particular commit.
 *
 * @author Sean Gilhooly
 */
public class GitHubPRCheckName implements Describable<GitHubPRCheckName> {

    private String checkName;

    @DataBoundConstructor
    public GitHubPRCheckName(final String checkName) {
        this.checkName = checkName;
    }

    /**
     * Return the name to use for the commit status context
     */
    public String getCheckName() {
        return checkName;
    }

    public Descriptor<GitHubPRCheckName> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(GitHubPRCheckName.class);
    }


    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRCheckName> {
        @Override
        public String getDisplayName() {
            return "Name for pull request status check";
        }
    }
}
