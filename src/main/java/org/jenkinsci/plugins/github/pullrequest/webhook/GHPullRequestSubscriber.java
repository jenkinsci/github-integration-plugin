package org.jenkinsci.plugins.github.pullrequest.webhook;

import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;

/**
 * @author lanwen (Merkushev Kirill)
 */
@SuppressWarnings("unused")
public class GHPullRequestSubscriber extends GHEventsSubscriber {
    private static Logger LOGGER = LoggerFactory.getLogger(GHPullRequestSubscriber.class);


    @Override
    protected boolean isApplicable(AbstractProject<?, ?> abstractProject) {
        return withTrigger(GitHubPRTrigger.class).apply(abstractProject);
    }

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PULL_REQUEST, GHEvent.ISSUE_COMMENT);
    }

    @Override
    protected void onEvent(GHEvent event, String payload) {
        try {
            GitHub gh = ((GitHubPRTrigger.DescriptorImpl) Jenkins.getInstance()
                    .getDescriptorOrDie(GitHubPRTrigger.class)).getGitHub();

            String repo = getRepo(event, payload, gh);

            for (AbstractProject<?, ?> job : getJobs(repo)) {
                GitHubPRTrigger trigger = job.getTrigger(GitHubPRTrigger.class);
                GitHubPRTriggerMode triggerMode = trigger.getTriggerMode();

                switch (triggerMode) {
                    case HEAVY_HOOKS:
                        trigger.queueRun(job);
                        break;

                    case LIGHT_HOOKS:
                        LOGGER.warn("Unsupported LIGHT_HOOKS trigger mode");
//                        LOGGER.log(Level.INFO, "Begin processing hooks for {0}", trigger.getRepoFullName(job));
//                        for (GitHubPREvent prEvent : trigger.getEvents()) {
//                            GitHubPRCause cause = prEvent.checkHook(trigger, parsedPayload, null);
//                            if (cause != null) {
//                                trigger.build(cause);
//                            }
//                        }
                        break;
                }
            }

        } catch (Exception e) {
            LOGGER.error("Can't process {} hook", event, e);
        }
    }

    private String getRepo(GHEvent event, String payload, GitHub gh) throws java.io.IOException {
        switch (event) {
            case ISSUE_COMMENT: {
                GHEventPayload.IssueComment commentPayload = gh.parseEventPayload(
                        new StringReader(payload), GHEventPayload.IssueComment.class);
                return commentPayload.getRepository().getFullName();
            }

            case PULL_REQUEST: {
                GHEventPayload.PullRequest pr = gh.parseEventPayload(
                        new StringReader(payload), GHEventPayload.PullRequest.class);
                return pr.getPullRequest().getRepository().getFullName();
            }

            default:
                LOGGER.warn("Did you add event {} in events() method?", event);
                return "";
        }
    }

    private Set<AbstractProject> getJobs(final String repo) {
        final Set<AbstractProject> ret = new HashSet<>();

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);
                ret.addAll(FluentIterableWrapper.from(jobs)
                        .filter(isBuildable())
                        .filter(withTrigger(GitHubPRTrigger.class))
                        .filter(new Predicate<AbstractProject>() {
                            @Override
                            public boolean apply(AbstractProject job) {
                                GitHubPRTrigger trigger = (GitHubPRTrigger) job.getTrigger(GitHubPRTrigger.class);
                                return trigger.getTriggerMode() != null
                                        && equalsIgnoreCase(repo, trigger.getRepoFullName(job));
                            }
                        }).toSet());
            }
        });

        return ret;
    }
}
