package org.jenkinsci.plugins.github.pullrequest.pipeline;

import java.io.Serializable;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;

/**
 * Representation of the configuration for the commit status set step. An instance of this class
 * is made available to the SetCommitStatusExecution object to instruct it on what to do.
 */
public class SetCommitStatusStep extends AbstractStepImpl implements Serializable {

    private static final long serialVersionUID = 1L;

    @DataBoundSetter
    private String context;

    @DataBoundSetter
    private GHCommitState state;

    @DataBoundSetter
    private String message;

    @DataBoundConstructor
    public SetCommitStatusStep() {
    }

    /**
     * The desired context for the status. The context identifies a status value on the commit. For
     * example, with two status values, one might have a context of "compile" and another
     * might have one with "tests" to indicate which phase of the build/validation the status
     * applies to.
     */
    public String getContext() {
        return context;
    }

    /**
     * The desired state of the status.
     */
    public GHCommitState getState() {
        return state;
    }

    /**
     * The message associated with the status providing some detail for it.
     */
    public String getMessage() {
        return message;
    }

    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        public static final String FUNC_NAME = "setGitHubPullRequestStatus";

        public DescriptorImpl() {
            super(SetCommitStatusExecution.class);
        }

        @Override
        public String getFunctionName() {
            return FUNC_NAME;
        }

        @Override
        public String getDisplayName() {
            return "Set GitHub PullRequest Commit Status";
        }
    }
}
