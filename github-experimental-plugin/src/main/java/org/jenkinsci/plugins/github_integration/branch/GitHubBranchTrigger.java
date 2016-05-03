package org.jenkinsci.plugins.github_integration.branch;

import antlr.ANTLRException;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubWebHook;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.Extension;
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
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;
import org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.jenkinsci.plugins.github_integration.branch.trigger.JobRunnerForCause;
import org.jenkinsci.plugins.github_integration.generic.GitHubAbstractTrigger;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHBranch;
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
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.notNull;
import static java.text.DateFormat.getDateTimeInstance;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger.DescriptorImpl.githubFor;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.CRON;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.LIGHT_HOOKS;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.webhook.WebhookInfoPredicates.withHookTriggerMode;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github_integration.branch.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.jenkinsci.plugins.github_integration.branch.trigger.check.NotUpdatedBranchFilter.notUpdated;
import static org.jenkinsci.plugins.github_integration.branch.trigger.check.PushToCauseConverter.toGitHubPushCause;
import static org.jenkinsci.plugins.github_integration.branch.trigger.check.SkipFirstRunForBranchFilter.ifSkippedFirstRun;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTrigger extends GitHubAbstractTrigger {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchTrigger.class);
    public static final String FINISH_MSG = "Finished GitHub Push trigger check";

    @CheckForNull
    private GitHubPRTriggerMode triggerMode = CRON;

    private boolean preStatus = false;
    private boolean cancelQueued = false;
    private boolean skipFirstRun = false;

    @CheckForNull
    private GitHubPRUserRestriction userRestriction;
    @CheckForNull
    private GitHubPRBranchRestriction branchRestriction;

    // for performance
    private transient GitHubRepositoryName repoName;
    private transient GHRepository remoteRepository;

    @CheckForNull
    private transient GitHubBranchPollingLogAction pollingLogAction;

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubBranchTrigger() {
    }

    @DataBoundConstructor
    public GitHubBranchTrigger(String spec, GitHubPRTriggerMode triggerMode) throws ANTLRException {
        super(spec);
        this.triggerMode = triggerMode;
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

    public GitHubPRUserRestriction getUserRestriction() {
        return userRestriction;
    }

    public GitHubPRBranchRestriction getBranchRestriction() {
        return branchRestriction;
    }

    @Override
    public void start(Job<?, ?> project, boolean newInstance) {
        LOGGER.info("Starting GitHub Push trigger for project {}", project.getName());
        super.start(project, newInstance);

        if (newInstance && GitHubPlugin.configuration().isManageHooks() && withHookTriggerMode().apply(project)) {
            GitHubWebHook.get().registerHookFor(project);
        }
    }

    public void run() {
        if (getTriggerMode() != LIGHT_HOOKS) {
            doRun(null);
        }
    }

    @Override
    public void stop() {
        //TODO clean hooks?
        if (nonNull(job)) {
            LOGGER.info("Stopping the Push trigger for project {}", job.getFullName());
        }
        super.stop();
    }

    @CheckForNull
    public GitHubBranchPollingLogAction getPollingLogAction() {
        if (isNull(pollingLogAction) && nonNull(job)) {
            pollingLogAction = new GitHubBranchPollingLogAction(job);
        }

        return pollingLogAction;
    }

    @Nonnull
    @Override
    public Collection<? extends Action> getProjectActions() {
        if (isNull(getPollingLogAction())) {
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
    public void queueRun(Job<?, ?> job, final String branch) {
        this.job = job;
        getDescriptor().queue.execute(new Runnable() {
            @Override
            public void run() {
                doRun(branch);
            }
        });
    }

    public GitHubRepositoryName getRepoFullName(Job<?, ?> job) {
        if (isNull(repoName)) {
            checkNotNull(job, "job object is null, race condition?");
            GithubProjectProperty ghpp = job.getProperty(GithubProjectProperty.class);

            checkNotNull(ghpp, "GitHub project property is not defined. Can't setup GitHub PR trigger for job %s",
                    job.getName());
            checkNotNull(ghpp.getProjectUrl(), "A GitHub project url is required");

            GitHubRepositoryName repo = GitHubRepositoryName.create(ghpp.getProjectUrl().baseUrl());

            checkNotNull(repo, "Invalid GitHub project url: %s", ghpp.getProjectUrl().baseUrl());

            repoName = repo;
        }

        return repoName;
    }

    public GHRepository getRemoteRepo() throws IOException {
        if (isNull(remoteRepository)) {
            Iterator<GHRepository> resolved = getRepoFullName(job).resolve().iterator();
            checkState(resolved.hasNext(), "Can't get remote GH repo for %s", job.getName());

            remoteRepository = resolved.next();
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
     * @param branch - branch for check, if null - then all PRs
     */
    public void doRun(String branch) {
        if (not(isBuildable()).apply(job)) {
            LOGGER.debug("Job {} is disabled, but trigger run!", isNull(job) ? "<no job>" : job.getFullName());
            return;
        }

        if (!isSupportedTriggerMode(getTriggerMode())) {
            LOGGER.warn("Trigger mode {} is not supported yet ({})", getTriggerMode(), job.getFullName());
            return;
        }

        GitHubBranchRepository localRepository = job.getAction(GitHubBranchRepository.class);
        if (isNull(localRepository)) {
            LOGGER.warn("Can't get repository info, maybe project {} misconfigured?", job.getFullName());
            return;
        }

        List<GitHubBranchCause> causes;

        try (LoggingTaskListenerWrapper listener =
                     new LoggingTaskListenerWrapper(getPollingLogAction().getPollingLogFile(), UTF_8)) {
            long startTime = System.currentTimeMillis();
            listener.debug("Running GitHub Pull Request trigger check for {} on {}",
                    getDateTimeInstance().format(new Date(startTime)), localRepository.getFullName());

            causes = readyToBuildCauses(localRepository, listener, branch);

            localRepository.saveQuetly();

            long duration = System.currentTimeMillis() - startTime;
            listener.info(FINISH_MSG + " for {} at {}. Duration: {}ms",
                    localRepository.getFullName(), getDateTimeInstance().format(new Date()), duration);
        } catch (Exception e) {
            LOGGER.error("Can't process check ({})", e.getMessage(), e);
            return;
        }

        from(causes).filter(new JobRunnerForCause(job, this)).toSet();
    }

    /**
     * @return list of causes for scheduling branch builds.
     */
    private List<GitHubBranchCause> readyToBuildCauses(GitHubBranchRepository localRepository,
                                                       LoggingTaskListenerWrapper listener,
                                                       @Nullable String branch) {
        try {
            GitHub github = githubFor(URI.create(localRepository.getGithubUrl()));
            GHRateLimit rateLimitBefore = github.getRateLimit();
            listener.debug("GitHub rate limit before check: {}", rateLimitBefore);

            // get local and remote list of branches
            GHRepository remoteRepo = getRemoteRepo();
            Set<GHBranch> remoteBranches = branchesToCheck(branch, remoteRepo, localRepository);

            Set<GHBranch> prepeared = from(remoteBranches)
                    .filter(notUpdated(localRepository, listener))
//                    .transform(prepareUserRestrictionFilter(localRepository, this))
                    .toSet();

            List<GitHubBranchCause> causes = from(prepeared)
                    .filter(and(
                            ifSkippedFirstRun(listener, skipFirstRun)//,
//                            withBranchRestriction(listener, branchRestriction),
//                            withUserRestriction(listener, userRestriction)
                    ))
                    .transform(toGitHubPushCause(localRepository, listener, this))
                    .filter(notNull())
                    .toList();

            LOGGER.trace("Causes count for {}: {}", localRepository.getFullName(), causes.size());
            from(prepeared).transform(updateLocalRepo(localRepository)).toSet();

            saveIfSkipFirstRun();

            GHRateLimit rateLimitAfter = github.getRateLimit();
            int consumed = rateLimitBefore.remaining - rateLimitAfter.remaining;
            LOGGER.info("GitHub rate limit after check {}: {}, consumed: {}, checked PRs: {}",
                    localRepository.getFullName(), rateLimitAfter, consumed, remoteBranches.size());

            return causes;
        } catch (IOException e) {
            listener.error("Can't get build causes, because: '{}'", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Set<GHBranch> branchesToCheck(String branch, GHRepository remoteRepo, GitHubBranchRepository localRepository)
            throws IOException {
        return (Set<GHBranch>) remoteRepo.getBranches().values();
    }

    private void saveIfSkipFirstRun() {
        if (skipFirstRun) {
            LOGGER.info("Skipping first run for {}", job.getFullName());
            skipFirstRun = false;
            trySave();
        }
    }

    private static boolean isSupportedTriggerMode(GitHubPRTriggerMode mode) {
        return mode != LIGHT_HOOKS;
    }

    @CheckForNull
    public Job<?, ?> getJob() {
        return job;
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private final transient SequentialExecutionQueue queue =
                new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && nonNull(SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item))
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            return "Build GitHub branches";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);

            save();
            return super.configure(req, formData);
        }


        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }
    }
}
