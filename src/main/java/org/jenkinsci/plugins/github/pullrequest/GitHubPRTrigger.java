package org.jenkinsci.plugins.github.pullrequest;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.google.common.base.Optional;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREventDescriptor;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.trigger.JobRunnerForCause;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRateLimit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static java.text.DateFormat.getDateTimeInstance;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.BranchRestrictionFilter.withBranchRestriction;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.NotUpdatedPRFilter.notUpdated;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter.toGitHubPRCause;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.SkipFirstRunForPRFilter.ifSkippedFirstRun;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionFilter.withUserRestriction;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.UserRestrictionPopulator.prepareUserRestrictionFilter;
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
 * - User restriction (check comments, labels, etc) {@link org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction}
 * (whitelist manipulations using comments is also allowed)
 * <p>
 * Event triggering is modular. Now they can be split to any events:
 * - Trigger by comment
 * - Trigger when PR opened
 * - Trigger when PR closed
 * - Trigger by label
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRTrigger extends Trigger<Job<?, ?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRTrigger.class);

    @CheckForNull
    private GitHubPRTriggerMode triggerMode = CRON;
    @CheckForNull
    private final List<GitHubPREvent> events;
    /**
     * Set PR(commit) status before build. No configurable message for it.
     */
    private boolean preStatus = false;
    private boolean cancelQueued = false;
    private boolean skipFirstRun = false;
    @CheckForNull
    private GitHubPRUserRestriction userRestriction;
    @CheckForNull
    private GitHubPRBranchRestriction branchRestriction;

    // for performance
    private transient String repoFullName;
    private transient GHRepository remoteRepository;

    @CheckForNull
    private transient GitHubPRPollingLogAction pollingLogAction;

    @DataBoundConstructor
    public GitHubPRTrigger(String spec,
                           GitHubPRTriggerMode triggerMode,
                           List<GitHubPREvent> events) throws ANTLRException {
        super(spec);
        this.triggerMode = triggerMode;
        this.events = Util.fixNull(events);
    }

    @DataBoundSetter
    public void setPreStatus(boolean preStatus) {
        this.preStatus = preStatus;
    }

    @DataBoundSetter
    public void setCancelQueued(boolean cancelQueued) {
        this.cancelQueued = cancelQueued;
    }

    @DataBoundSetter
    public void setSkipFirstRun(boolean skipFirstRun) {
        this.skipFirstRun = skipFirstRun;
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

    public boolean isCancelQueued() {
        return cancelQueued;
    }

    public boolean isSkipFirstRun() {
        return skipFirstRun;
    }

    public GitHubPRTriggerMode getTriggerMode() {
        return triggerMode;
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
    public void start(Job<?, ?> project, boolean newInstance) {
        LOGGER.info("Starting GitHub Pull Request trigger for project {}", project.getName());
        super.start(project, newInstance);

        if (newInstance && GitHubPlugin.configuration().isManageHooks() && withHookTriggerMode().apply(project)) {
            GitHubWebHook.get().registerHookFor(project);
        }
    }

    @Override
    public void run() {
        if (CRON == getTriggerMode()) {
            doRun(null);
        }
    }

    @Override
    public void stop() {
        //TODO clean hooks?
        if (job != null) {
            LOGGER.info("Stopping the GitHub PR trigger for project {}", job.getFullName());
        }
        super.stop();
    }

    @CheckForNull
    public GitHubPRPollingLogAction getPollingLogAction() {
        if (pollingLogAction == null && job != null) {
            pollingLogAction = new GitHubPRPollingLogAction(job);
        }

        return pollingLogAction;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (getPollingLogAction() == null) {
            return Collections.emptyList();
        }
        return Collections.singleton(getPollingLogAction());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * For running from external places. Goes to queue.
     */
    public void queueRun(AbstractProject<?, ?> job, final int prNumber) {
        this.job = job;
        getDescriptor().queue.execute(new Runnable() {
            @Override
            public void run() {
                doRun(prNumber);
            }
        });
    }

    public String getRepoFullName(Job<?, ?> job) {
        if (isBlank(repoFullName)) {

            checkNotNull(job, "job object is null, race condition?");
            GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);

            checkNotNull(ghpp, "GitHub project property is not defined. Can't setup GitHub PR trigger for job %s", job.getName());
            checkNotNull(ghpp.getProjectUrl(), "A GitHub project url is required");

            GitHubRepositoryName repo = GitHubRepositoryName.create(ghpp.getProjectUrl().baseUrl());

            checkNotNull(repo, "Invalid GitHub project url: %s", ghpp.getProjectUrl().baseUrl());

            repoFullName = String.format("%s/%s", repo.getUserName(), repo.getRepositoryName());
        }

        return repoFullName;
    }

    public GHRepository getRemoteRepo() throws IOException {
        if (remoteRepository == null) {
            String repo = getRepoFullName(job);
            GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);

            remoteRepository = DescriptorImpl.githubFor(URI.create(ghpp.getProjectUrl().baseUrl()))
                    .getRepository(repo);
        }
        return remoteRepository;
    }

    public void trySave() {
        try {
            job.save();
        } catch (IOException e) {
            LOGGER.error("Error while saving job to file", e);
        }
    }

    /**
     * Runs check
     *
     * @param prNumber - PR number for check, if null - then all PRs
     */
    private void doRun(Integer prNumber) {
        if (not(isBuildable()).apply(job)) {
            LOGGER.debug("Job {} is disabled, but trigger run!", job == null ? "<no job>" : job.getFullName());
            return;
        }

        if (!isSupportedTriggerMode(getTriggerMode())) {
            LOGGER.warn("Trigger mode {} is not supported yet ({})", getTriggerMode(), job.getFullName());
            return;
        }

        GitHubPRRepository localRepository = job.getAction(GitHubPRRepository.class);
        if (localRepository == null) {
            LOGGER.warn("Can't get repository info, maybe project {} misconfigured?", job.getFullName());
            return;
        }

        List<GitHubPRCause> causes;

        try (LoggingTaskListenerWrapper listener =
                     new LoggingTaskListenerWrapper(getPollingLogAction().getLogFile(), UTF_8)) {
            long startTime = System.currentTimeMillis();
            listener.debug("Running GitHub Pull Request trigger check for {} on {}",
                    getDateTimeInstance().format(new Date(startTime)), localRepository.getFullName());

            causes = readyToBuildCauses(localRepository, listener, prNumber);

            localRepository.saveQuetly();

            long duration = System.currentTimeMillis() - startTime;
            listener.info("Finished GitHub Pull Request trigger check for {} at {}. Duration: {}ms",
                    localRepository.getFullName(), getDateTimeInstance().format(new Date()), duration);
        } catch (Exception e) {
            LOGGER.error("Can't process check ({})", e.getMessage(), e);
            return;
        }

        from(causes).filter(new JobRunnerForCause(job, this)).toSet();
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
     *
     * @return causes which ready to be converted to job-starts. One cause per repo.
     */
    private List<GitHubPRCause> readyToBuildCauses(GitHubPRRepository localRepository,
                                                   LoggingTaskListenerWrapper listener,
                                                   @Nullable Integer prNumber) {
        try {
            GitHub github = DescriptorImpl.githubFor(URI.create(localRepository.getGithubUrl()));
            GHRateLimit rateLimitBefore = github.getRateLimit();
            listener.debug("GitHub rate limit before check: {}", rateLimitBefore);

            // get local and remote list of PRs
            GHRepository remoteRepository = github.getRepository(getRepoFullName(job));
            Set<GHPullRequest> remotePulls = pullRequestsToCheck(prNumber, remoteRepository, localRepository);

            Set<GHPullRequest> prepeared = from(remotePulls)
                    .filter(notUpdated(localRepository, listener))
                    .transform(prepareUserRestrictionFilter(localRepository, this)).toSet();

            List<GitHubPRCause> causes = from(prepeared)
                    .filter(and(
                            ifSkippedFirstRun(listener, skipFirstRun),
                            withBranchRestriction(listener, branchRestriction),
                            withUserRestriction(listener, userRestriction)
                    ))
                    .transform(toGitHubPRCause(localRepository, listener, this))
                    .filter(notNull()).toList();

            LOGGER.trace("Causes count for {}: {}", localRepository.getFullName(), causes.size());
            from(prepeared).transform(updateLocalRepo(localRepository)).toSet();

            saveIfSkipFirstRun();

            GHRateLimit rateLimitAfter = github.getRateLimit();
            int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
            LOGGER.info("GitHub rate limit after check {}: {}, consumed: {}, checked PRs: {}",
                    localRepository.getFullName(), rateLimitAfter, consumed, remotePulls.size());

            return causes;
        } catch (IOException e) {
            listener.error("Can't get build causes, because: '{}'", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void saveIfSkipFirstRun() {
        if (skipFirstRun) {
            LOGGER.info("Skipping first run for {}", job.getFullName());
            skipFirstRun = false;
            trySave();
        }
    }

    private static boolean isSupportedTriggerMode(GitHubPRTriggerMode mode) {
        return mode == CRON || mode == HEAVY_HOOKS;
    }

    private static Set<GHPullRequest> pullRequestsToCheck(@Nullable Integer prNumber,
                                                          GHRepository remoteRepo,
                                                          GitHubPRRepository localRepo) throws IOException {
        if (prNumber != null) {
            return Collections.singleton(remoteRepo.getPullRequest(prNumber));
        } else {
            List<GHPullRequest> remotePulls = remoteRepo.getPullRequests(GHIssueState.OPEN);

            Set<Integer> remotePRNums = from(remotePulls).transform(extractPRNumber()).toSet();

            return from(localRepo.getPulls().keySet())
                    // add PRs that was closed on remote
                    .filter(not(in(remotePRNums)))
                    .transform(fetchRemotePR(remoteRepo)).filter(notNull()).append(remotePulls).toSet();
        }
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private final transient SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        private String whitelistUserMsg = ".*add\\W+to\\W+whitelist.*";
        private String spec = "H/5 * * * *";

        private String publishedURL;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            return "Build GitHub pull requests";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
//            req.bindJSON(this, formData);
            publishedURL = formData.getString("publishedURL");
            whitelistUserMsg = formData.getString("whitelistUserMsg");
            spec = formData.getString("spec");

            save();
            return super.configure(req, formData);
        }

        public String getPublishedURL() {
            return publishedURL;
        }

        public String getJenkinsURL() {
            String url = getPublishedURL();
            if (isNotBlank(url)) {
                if (!url.endsWith("/")) {
                    url += "/";
                }
                return url;
            }
            return GitHubWebHook.getJenkinsInstance().getRootUrl();
        }

        public String getWhitelistUserMsg() {
            return whitelistUserMsg;
        }

        public String getSpec() {
            return spec;
        }

        // list all available descriptors for choosing in job configuration
        public List<GitHubPREventDescriptor> getEventDescriptors() {
            return GitHubPREventDescriptor.all();
        }

        @Nonnull
        public static GitHub githubFor(URI uri) {
            Optional<GitHub> client = from(GitHubPlugin.configuration()
                    .findGithubConfig(withHost(uri.getHost()))).first();
            if (client.isPresent()) {
                return client.get();
            } else {
                throw new GHPluginConfigException("Can't find appropriate client for github repo <%s>", uri);
            }
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }
    }
}
