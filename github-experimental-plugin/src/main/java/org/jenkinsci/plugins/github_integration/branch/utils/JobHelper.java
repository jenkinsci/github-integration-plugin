package org.jenkinsci.plugins.github_integration.branch.utils;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.github_integration.branch.GitHubBranchCause;
import org.jenkinsci.plugins.github_integration.branch.GitHubBranchTrigger;

import javax.annotation.CheckForNull;

import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghCauseFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.triggerFrom;

/**
 * @author Kanstantsin Shautsou
 */
public class JobHelper {
    private JobHelper() {
    }

    @CheckForNull
    public static GitHubBranchTrigger ghBranchTiggerFromRun(Run<?, ?> run) {
        return triggerFrom(run.getParent(), GitHubBranchTrigger.class);
    }

    @CheckForNull
    public static GitHubBranchTrigger ghBranchTriggerFromJob(Job<?, ?> job) {
        return triggerFrom(job, GitHubBranchTrigger.class);
    }

    @CheckForNull
    public static GitHubBranchCause ghBranchCauseFromRun(Run<?, ?> run) {
        return ghCauseFromRun(run, GitHubBranchCause.class);
    }

}
