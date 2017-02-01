package org.jenkinsci.plugins.github.pullrequest.dsl;

import antlr.ANTLRException;
import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.dsl.helpers.triggers.TriggerContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.GitHubPRTriggerDslContext;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.publishers.GitHubCommentPublisherDslContext;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.publishers.GitHubPRStatusPublisherDslContext;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.steps.GitHubPRStatusStepDslContext;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Extension(optional = true)
public class GitHubPRJobDslExtension extends ContextExtensionPoint {

    @DslExtensionMethod(context = TriggerContext.class)
    public Object onPullRequest(Runnable closure) throws ANTLRException {

        GitHubPRTriggerDslContext context = new GitHubPRTriggerDslContext();
        executeInContext(closure, context);

        GitHubPRTrigger trigger = new GitHubPRTrigger(context.cron(), context.mode(), context.events());
        trigger.setPreStatus(context.isSetPreStatus());
        trigger.setCancelQueued(context.isCancelQueued());
        trigger.setAbortRunning(context.isAbortRunning());
        return trigger;
    }

    @DslExtensionMethod(context = PublisherContext.class)
    public Object commitStatusOnGH(Runnable closure) {

        GitHubPRStatusPublisherDslContext context = new GitHubPRStatusPublisherDslContext();
        executeInContext(closure, context);

        return new GitHubPRBuildStatusPublisher(
                context.message(),
                context.unstableAs(),
                null,
                null,
                null
        );
    }

    @DslExtensionMethod(context = PublisherContext.class)
    public Object commentPullRequestOnGH() {
        return GitHubCommentPublisherDslContext.DEFAULT_PUBLISHER;
    }

    @DslExtensionMethod(context = PublisherContext.class)
    public Object commentPullRequestOnGH(Runnable closure) {
        GitHubCommentPublisherDslContext context = new GitHubCommentPublisherDslContext();
        executeInContext(closure, context);

        return context.getPublisher();
    }

    @DslExtensionMethod(context = StepContext.class)
    public Object updateStatusOnGH(Runnable closure) {
        GitHubPRStatusStepDslContext context = new GitHubPRStatusStepDslContext();
        executeInContext(closure, context);

        return new GitHubPRStatusBuilder(context.message());
    }
}
