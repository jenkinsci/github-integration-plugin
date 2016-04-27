package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixProject;
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
import jenkins.model.ParameterizedJobMixIn;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRBadgeAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.kohsuke.github.GHCommitState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cloudbees.jenkins.GitHubWebHook.getJenkinsInstance;
import static com.google.common.base.Predicates.instanceOf;
import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.AUTHOR_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.CAUSE_SKIP;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COMMIT_AUTHOR_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COMMIT_AUTHOR_NAME;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.COND_REF;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.HEAD_SHA;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.NUMBER;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SHORT_DESC;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SOURCE_BRANCH;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.SOURCE_REPO_OWNER;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TARGET_BRANCH;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TITLE;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TRIGGER_SENDER_AUTHOR;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.TRIGGER_SENDER_EMAIL;
import static org.jenkinsci.plugins.github.pullrequest.data.GitHubPREnv.URL;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class JobRunnerForCause implements Predicate<GitHubPRCause> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunnerForCause.class);

    private Job<?, ?> job;
    private GitHubPRTrigger trigger;

    public JobRunnerForCause(Job<?, ?> job, GitHubPRTrigger trigger) {
        this.job = job;
        this.trigger = trigger;
    }

    @Override
    public boolean apply(GitHubPRCause cause) {
        try {
            cause.setPollingLog(trigger.getPollingLogAction().getLogFile());

            StringBuilder sb = new StringBuilder();
            sb.append("Jenkins queued the run (").append(cause.getReason()).append(")");

            if (trigger.isCancelQueued() && cancelQueuedBuildByPrNumber(cause.getNumber())) {
                sb.append(". Queued builds aborted");
            }

            QueueTaskFuture<?> queueTaskFuture = startJob(cause);
            if (isNull(queueTaskFuture)) {
                LOGGER.error("{} job didn't start", job.getFullName());
            }

            LOGGER.info(sb.toString());

            // remote connection
            if (trigger.isPreStatus()) {
                if (job instanceof MatrixProject) {
                    Collection<? extends MatrixConfiguration> configs = ((MatrixProject) job).getActiveConfigurations();
                    for (MatrixConfiguration config : configs) {
                        trigger.getRemoteRepo()
                                .createCommitStatus(cause.getHeadSha(),
                                        GHCommitState.PENDING,
                                        config.getAbsoluteUrl(),
                                        sb.toString(),
                                        config.getFullName());
                    }
                } else {
                    trigger.getRemoteRepo()
                            .createCommitStatus(cause.getHeadSha(),
                                    GHCommitState.PENDING,
                                    job.getAbsoluteUrl(),
                                    sb.toString(),
                                    job.getFullName());
                }
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
    private static boolean cancelQueuedBuildByPrNumber(final int id) {
        Queue queue = getJenkinsInstance().getQueue();

        for (Queue.Item item : queue.getApproximateItemsQuickly()) {
            Optional<Cause> cause = from(item.getAllActions())
                    .filter(instanceOf(CauseAction.class))
                    .transformAndConcat(new CausesFromAction())
                    .filter(instanceOf(GitHubPRCause.class))
                    .firstMatch(new CauseHasPRNum(id));

            if (cause.isPresent()) {
                queue.cancel(item);
                return true;
            }
        }

        return false;
    }

    private QueueTaskFuture<?> startJob(GitHubPRCause cause) {
        List<ParameterValue> values = getDefaultParametersValues();
        values.addAll(asList(
                TRIGGER_SENDER_AUTHOR.param(cause.getTriggerSenderName()),
                TRIGGER_SENDER_EMAIL.param(cause.getTriggerSenderEmail()),
                COMMIT_AUTHOR_NAME.param(cause.getCommitAuthorName()),
                COMMIT_AUTHOR_EMAIL.param(cause.getCommitAuthorEmail()),
                TARGET_BRANCH.param(cause.getTargetBranch()),
                SOURCE_BRANCH.param(cause.getSourceBranch()),
                AUTHOR_EMAIL.param(cause.getPRAuthorEmail()),
                SHORT_DESC.param(cause.getShortDescription()),
                TITLE.param(cause.getTitle()),
                URL.param(cause.getHtmlUrl().toString()),
                SOURCE_REPO_OWNER.param(cause.getSourceRepoOwner()),
                HEAD_SHA.param(cause.getHeadSha()),
                COND_REF.param(cause.getCondRef()),
                CAUSE_SKIP.param(cause.isSkip()),
                NUMBER.param(String.valueOf(cause.getNumber()))
        ));
        GitHubPRBadgeAction gitHubPRBadgeAction = new GitHubPRBadgeAction(cause);

        ParameterizedJobMixIn parameterizedJobMixIn = asParameterizedJobMixIn(job);

        // remove after switch to newer core
        int quietPeriod = 0;
        try {
            Object mixinJob = FieldUtils.readField(parameterizedJobMixIn, "val$job", true);
            ParameterizedJobMixIn.ParameterizedJob parameterizedJob = (ParameterizedJobMixIn.ParameterizedJob) mixinJob;
            quietPeriod = parameterizedJob.getQuietPeriod();
        } catch (IllegalAccessException e) {
            LOGGER.error("Couldn't extract quiet period, falling back to {}", quietPeriod, e);
        }

        return parameterizedJobMixIn.scheduleBuild2(quietPeriod, new CauseAction(cause), new ParametersAction(values),
                gitHubPRBadgeAction);
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

    private static class CauseHasPRNum implements Predicate<Cause> {
        private final int id;

        CauseHasPRNum(int id) {
            this.id = id;
        }

        @Override
        public boolean apply(Cause cause) {
            return ((GitHubPRCause) cause).getNumber() == id;
        }
    }
}
