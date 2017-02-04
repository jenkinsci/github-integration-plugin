package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.github.kostyasha.github.integration.generic.GitHubTrigger;
import com.github.kostyasha.github.integration.generic.GitHubTriggerDescriptor;
import hudson.Extension;
import hudson.Util;
import hudson.model.Job;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.trigger.JobRunnerForCause;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static java.text.DateFormat.getDateTimeInstance;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static com.github.kostyasha.github.integration.generic.utils.RetryableGitHubOperation.execute;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.LIGHT_HOOKS;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.BranchRestrictionFilter.withBranchRestriction;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.NotUpdatedPRFilter.notUpdated;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter.toGitHubPRCause;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipFirstRunForPRFilter.ifSkippedFirstRun;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipPRInBadState.badState;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionFilter.withUserRestriction;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionPopulator.prepareUserRestrictionFilter;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.extractPRNumber;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.fetchRemotePR;
import static org.jenkinsci.plugins.github.pullrequest.webhook.WebhookInfoPredicates.withHookTriggerMode;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;

/**
 * GitHub Pull Request trigger.
 * Planned trigger modes:
 * - just incoming hooks, without persist (save PR state to local xml)
 * - hooks with persist
 * - cron run, persist
 * <p>
 * Restrictions can't have resolver, so they separate and provide security check methods:
 * - Target branch restriction {@link org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction}
 * - User restriction (check comments, labels, etc)
 * {@link org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction}
 * (whitelist manipulations using comments is also allowed)
 * <p>
 * Event triggering is modular. Now they can be split to any events:
 * - Trigger by comment
 * - Trigger when PR opened
 * - Trigger when PR closed
 * - Trigger by label
 * - etc.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRTrigger extends GitHubTrigger<GitHubPRTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRTrigger.class);
    public static final String FINISH_MSG = "Finished GitHub Pull Request trigger check";

    @CheckForNull
    private List<GitHubPREvent> events = new ArrayList<>();
    /**
     * Set PR(commit) status before build. No configurable message for it.
     */
    private boolean preStatus = false;

    @CheckForNull
    private GitHubPRUserRestriction userRestriction;
    @CheckForNull
    private GitHubPRBranchRestriction branchRestriction;

    // for performance
    private transient GitHubRepositoryName repoName;
    private transient GHRepository remoteRepository;

    @CheckForNull
    private transient GitHubPRPollingLogAction pollingLogAction;

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubPRTrigger() throws ANTLRException {
        super("");
    }

    @DataBoundConstructor
    public GitHubPRTrigger(String spec,
                           GitHubPRTriggerMode triggerMode,
                           List<GitHubPREvent> events) throws ANTLRException {
        super(spec);
        setTriggerMode(triggerMode);
        this.events = Util.fixNull(events);
    }

    @DataBoundSetter
    public void setPreStatus(boolean preStatus) {
        this.preStatus = preStatus;
    }

    @DataBoundSetter
    public void setUserRestriction(GitHubPRUserRestriction userRestriction) {
        this.userRestriction = userRestriction;
    }

    @DataBoundSetter
    public void setBranchRestriction(GitHubPRBranchRestriction branchRestriction) {
        this.branchRestriction = branchRestriction;
    }

    public boolean isPreStatus() {
        return preStatus;
    }

    public List<GitHubPREvent> getEvents() {
        return events;
    }

    public GitHubPRUserRestriction getUserRestriction() {
        return userRestriction;
    }

    public GitHubPRBranchRestriction getBranchRestriction() {
        return branchRestriction;
    }

    @Override
    public void start(Job<?, ?> job, boolean newInstance) {
        LOGGER.info("Starting GitHub Pull Request trigger for project {}", job.getFullName());
        super.start(job, newInstance);

        if (newInstance
                && getRepoProvider().isManageHooks(this)
                && withHookTriggerMode().apply(job)) {
            getRepoProvider().registerHookFor(this);
        }
    }

    @Override
    public void run() {
        if (getTriggerMode() != LIGHT_HOOKS) {
            doRun(null);
        }
    }


    @CheckForNull
    @Override
    public GitHubPRPollingLogAction getPollingLogAction() {
        if (isNull(pollingLogAction) && nonNull(job)) {
            pollingLogAction = new GitHubPRPollingLogAction(job);
        }

        return pollingLogAction;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) Jenkins.getActiveInstance().getDescriptor(this.getClass());
    }

    /**
     * For running from external places. Goes to queue.
     */
    public void queueRun(Job<?, ?> job, final int prNumber) {
        this.job = job;
        getDescriptor().getQueue().execute(() -> doRun(prNumber));
    }

    /**
     * Runs check
     *
     * @param prNumber - PR number for check, if null - then all PRs
     */
    public void doRun(Integer prNumber) {
        if (not(isBuildable()).apply(job)) {
            LOGGER.debug("Job {} is disabled, but trigger run!", isNull(job) ? "<no job>" : job.getFullName());
            return;
        }

        if (!isSupportedTriggerMode(getTriggerMode())) {
            LOGGER.warn("Trigger mode {} is not supported yet ({})", getTriggerMode(), job.getFullName());
            return;
        }

        GitHubPRRepository localRepository = job.getAction(GitHubPRRepository.class);
        if (isNull(localRepository)) {
            LOGGER.warn("Can't get repository info, maybe project {} misconfigured?", job.getFullName());
            return;
        }

        List<GitHubPRCause> causes;

        try (LoggingTaskListenerWrapper listener =
                     new LoggingTaskListenerWrapper(getPollingLogAction().getPollingLogFile(), UTF_8)) {
            long startTime = System.currentTimeMillis();
            listener.debug("Running GitHub Pull Request trigger check for {} on {}",
                    getDateTimeInstance().format(new Date(startTime)), localRepository.getFullName());
            try {
                localRepository.actualise(getRemoteRepository());

                causes = readyToBuildCauses(localRepository, listener, prNumber);

                localRepository.saveQuietly();

                // TODO print triggering to listener?
                from(causes).filter(new JobRunnerForCause(job, this)).toSet();
            } catch (Throwable t) {
                listener.error("Can't end trigger check", t);
            }

            long duration = System.currentTimeMillis() - startTime;
            listener.info(FINISH_MSG + " for {} at {}. Duration: {}ms",
                    localRepository.getFullName(), getDateTimeInstance().format(new Date()), duration);
        } catch (Exception e) {
            // out of UI/user viewable error
            LOGGER.error("Can't process check ({})", e.getMessage(), e);
        }
    }

    /**
     * runs check of local (last) Repository state (list of PRs) vs current remote state
     * - local state store only last open PRs
     * - if last open PR <-> now closed -> should trigger only when ClosePREvent exist
     * - last open PR <-> now changed -> trigger only
     * - special comment in PR -> trigger
     *
     * @param localRepository persisted data to compare with remote state
     * @param listener        logger to write to console and to polling log
     * @param prNumber        pull request number to fetch only required num. Can be null
     * @return causes which ready to be converted to job-starts. One cause per repo.
     */
    private List<GitHubPRCause> readyToBuildCauses(@Nonnull GitHubPRRepository localRepository,
                                                   @Nonnull LoggingTaskListenerWrapper listener,
                                                   @Nullable Integer prNumber) {
        try {
            GitHub github = getRepoProvider().getGitHub(this);

            GHRateLimit rateLimitBefore = github.getRateLimit();
            listener.debug("GitHub rate limit before check: {}", rateLimitBefore);

            // get local and remote list of PRs
            //FIXME HiddenField: 'remoteRepository' hides a field? renamed to `remoteRepo`
            GHRepository remoteRepo = getRemoteRepository();
            Set<GHPullRequest> remotePulls = pullRequestsToCheck(prNumber, remoteRepo, localRepository);

            Set<GHPullRequest> prepared = from(remotePulls)
                    .filter(badState(localRepository, listener))
                    .filter(notUpdated(localRepository, listener))
                    .transform(prepareUserRestrictionFilter(localRepository, this))
                    .toSet();

            List<GitHubPRCause> causes = from(prepared)
                    .filter(and(
                            ifSkippedFirstRun(listener, isSkipFirstRun()),
                            withBranchRestriction(listener, getBranchRestriction()),
                            withUserRestriction(listener, getUserRestriction())
                    ))
                    .transform(toGitHubPRCause(localRepository, listener, this))
                    .filter(notNull())
                    .toList();

            LOGGER.trace("Causes count for {}: {}", localRepository.getFullName(), causes.size());

            // refresh all PRs because user may add events that may trigger unexpected builds.
            from(remotePulls).transform(updateLocalRepo(localRepository)).toSet();

            saveIfSkipFirstRun();

            GHRateLimit rateLimitAfter = github.getRateLimit();
            int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
            LOGGER.info("GitHub rate limit after check {}: {}, consumed: {}, checked PRs: {}",
                    localRepository.getFullName(), rateLimitAfter, consumed, remotePulls.size());

            return causes;
        } catch (IOException e) {
            listener.error("Can't get build causes: ", e);
            return emptyList();
        }
    }

    private static boolean isSupportedTriggerMode(GitHubPRTriggerMode mode) {
        return mode != LIGHT_HOOKS;
    }

    /**
     * @return remote pull requests for future analysing.
     */
    private static Set<GHPullRequest> pullRequestsToCheck(@Nullable Integer prNumber,
                                                          @Nonnull GHRepository remoteRepo,
                                                          @Nonnull GitHubPRRepository localRepo) throws IOException {
        if (prNumber != null) {
            return execute(() -> singleton(remoteRepo.getPullRequest(prNumber)));
        } else {
            List<GHPullRequest> remotePulls = execute(() -> remoteRepo.getPullRequests(GHIssueState.OPEN));

            Set<Integer> remotePRNums = from(remotePulls).transform(extractPRNumber()).toSet();

            return from(localRepo.getPulls().keySet())
                    // add PRs that was closed on remote
                    .filter(not(in(remotePRNums)))
                    .transform(fetchRemotePR(remoteRepo))
                    .filter(notNull())
                    .append(remotePulls)
                    .toSet();
        }
    }

    @Override
    public String getFinishMsg() {
        return FINISH_MSG;
    }

    @Extension
    public static class DescriptorImpl extends GitHubTriggerDescriptor {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "Build GitHub Pull Requests";
        }

        // list all available descriptors for choosing in job configuration
        public List<GitHubPREventDescriptor> getEventDescriptors() {
            return GitHubPREventDescriptor.all();
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }
    }
}
