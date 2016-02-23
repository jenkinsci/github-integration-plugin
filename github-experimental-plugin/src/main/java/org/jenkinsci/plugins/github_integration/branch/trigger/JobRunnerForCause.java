package org.jenkinsci.plugins.github_integration.branch.trigger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue;
import hudson.model.queue.QueueTaskFuture;
import org.jenkinsci.plugins.github_integration.branch.GitHubBranchCause;
import org.jenkinsci.plugins.github_integration.branch.GitHubBranchTrigger;
import org.kohsuke.github.GHCommitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.cloudbees.jenkins.GitHubWebHook.getJenkinsInstance;
import static com.google.common.base.Predicates.instanceOf;
import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.HEAD_SHA;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SHORT_DESC;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SOURCE_BRANCH;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCause implements Predicate<GitHubBranchCause> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForCause.class);

    private Job<?, ?> job;
    private GitHubBranchTrigger trigger;

    public JobRunnerForCause(Job<?, ?> job, GitHubBranchTrigger trigger) {
        this.job = job;
        this.trigger = trigger;
    }

    @Override
    public boolean apply(GitHubBranchCause cause) {
        try {
            cause.setPollingLog(trigger.getPollingLogAction().getLogFile());

            StringBuilder sb = new StringBuilder();
            sb.append("Jenkins queued the run (").append(cause.getReason()).append(")");

            if (trigger.isCancelQueued() && cancelQueuedBuildByBranchName(cause.getSourceBranch())) {
                sb.append(". Queued builds aborted");
            }

            QueueTaskFuture<?> queueTaskFuture = startJob(cause);
            if (isNull(queueTaskFuture)) {
                LOGGER.error("{} job didn't start", job.getFullName());
            }

            LOGGER.info(sb.toString());

            // remote connection
            if (trigger.isPreStatus()) {
                trigger.getRemoteRepo()
                        .createCommitStatus(cause.getHeadSha(),
                                GHCommitState.PENDING,
                                null,
                                sb.toString(),
                                job.getFullName());
            }
        } catch (IOException e) {
            LOGGER.error("Can't trigger build ({})", e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Cancel previous builds for specified PR id.
     */
    private static boolean cancelQueuedBuildByBranchName(final String branch) {
        Queue queue = getJenkinsInstance().getQueue();

        for (Queue.Item item : queue.getApproximateItemsQuickly()) {
            Optional<Cause> cause = from(item.getAllActions())
                    .filter(instanceOf(CauseAction.class))
                    .transformAndConcat(new CausesFromAction())
                    .filter(instanceOf(GitHubBranchCause.class))
                    .firstMatch(new CauseHasBranch(branch));

            if (cause.isPresent()) {
                queue.cancel(item);
                return true;
            }
        }

        return false;
    }

    private QueueTaskFuture<?> startJob(GitHubBranchCause cause) {
        List<ParameterValue> values = getDefaultParametersValues();
        values.addAll(asList(
//                TRIGGER_SENDER_AUTHOR.param(cause.getTriggerSenderName()),
//                TRIGGER_SENDER_EMAIL.param(cause.getTriggerSenderEmail()),
//                COMMIT_AUTHOR_NAME.param(cause.getCommitAuthorName()),
//                COMMIT_AUTHOR_EMAIL.param(cause.getCommitAuthorEmail()),
                SOURCE_BRANCH.param(cause.getSourceBranch()),
//                AUTHOR_EMAIL.param(cause.getPRAuthorEmail()),
                SHORT_DESC.param(cause.getShortDescription()),
//                TITLE.param(cause.getTitle()),
//                URL.param(cause.getHtmlUrl().toString()),
//                SOURCE_REPO_OWNER.param(cause.getSourceRepoOwner()),
                HEAD_SHA.param(cause.getHeadSha())//,
//                CAUSE_SKIP.param(cause.isSkip()),
        ));
        //TODO no way to get quietPeriod, so temporary ignore it
        return asParameterizedJobMixIn(job).scheduleBuild2(0, new CauseAction(cause), new ParametersAction(values));
    }

    /**
     * @see jenkins.model.ParameterizedJobMixIn#getDefaultParametersValues()
     */
    private List<ParameterValue> getDefaultParametersValues() {
        ParametersDefinitionProperty paramDefProp = job.getProperty(ParametersDefinitionProperty.class);
        List<ParameterValue> defValues = new ArrayList<>();

        /*
        * This check is made ONLY if someone will call this method even if isParametrized() is false.
        */
        if (paramDefProp == null) {
            return defValues;
        }

        /* Scan for all parameter with an associated default values */
        for (ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions()) {
            ParameterValue defaultValue = paramDefinition.getDefaultParameterValue();

            if (defaultValue != null) {
                defValues.add(defaultValue);
            }
        }

        return defValues;
    }

    private static class CausesFromAction implements Function<Action, Iterable<Cause>> {
        @Override
        public Iterable<Cause> apply(Action input) {
            return ((CauseAction) input).getCauses();
        }
    }

    private static class CauseHasBranch implements Predicate<Cause> {
        private final String branch;

        CauseHasBranch(String branch) {
            this.branch = branch;
        }

        @Override
        public boolean apply(Cause cause) {
            return ((GitHubBranchCause) cause).getSourceBranch().equals(branch);
        }
    }
}
