package org.jenkinsci.plugins.github.pullrequest.utils;

import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.Job;
import hudson.model.Run;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;

import javax.annotation.CheckForNull;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public class JobHelper {
    private JobHelper() {
    }

    @CheckForNull
    public static GitHubPRTrigger ghPRTriggerFromRun(Run<?, ?> run) {
        return triggerFrom(run.getParent(), GitHubPRTrigger.class);
    }

    @CheckForNull
    public static GitHubPRTrigger ghPRTriggerFromJob(Job<?, ?> job) {
        return triggerFrom(job, GitHubPRTrigger.class);
    }

    /**
     * support matrix plugin.
     * @see JobInfoHelpers#triggerFrom(hudson.model.Job, java.lang.Class)
     */
    @CheckForNull
    public static <T extends Trigger> T triggerFrom(final Job<?, ?> job, Class<T> tClass) {
        Job<?, ?> guessJob;
        if (job instanceof MatrixConfiguration) {
            guessJob = ((MatrixConfiguration) job).getParent();
        } else {
            guessJob = job;
        }

        if (guessJob instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) guessJob;

            for (Trigger candidate : pJob.getTriggers().values()) {
                if (tClass.isInstance(candidate)) {
                    return tClass.cast(candidate);
                }
            }
        }
        return null;
    }

    /**
     * matrix-project requires special extraction.
     */
    @CheckForNull
    public static GitHubPRCause ghPRCauseFromRun(Run <?, ?> run) {
        GitHubPRCause cause = null;
        if (run instanceof MatrixRun) {
            MatrixBuild parentBuild = ((MatrixRun) run).getParentBuild();
            if (nonNull(parentBuild)) {
                cause = parentBuild.getCause(GitHubPRCause.class);
            }
        } else {
            cause = run.getCause(GitHubPRCause.class);
        }

        return cause;
    }
}
