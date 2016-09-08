package com.github.kostyasha.github.integration.branch.dsl;

import antlr.ANTLRException;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.dsl.context.GitHubBranchTriggerDslContext;
import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.triggers.TriggerContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

/**
 * @author Kanstantsin Shautsou
 */
@Extension(optional = true)
public class GitHubBranchJobDslExtenstion extends ContextExtensionPoint {
    @DslExtensionMethod(context = TriggerContext.class)
    public Object onBranch(Runnable closure) throws ANTLRException {

        GitHubBranchTriggerDslContext context = new GitHubBranchTriggerDslContext();
        executeInContext(closure, context);

        GitHubBranchTrigger trigger = new GitHubBranchTrigger(context.cron(), context.mode(), context.events());
        trigger.setPreStatus(context.isSetPreStatus());
        trigger.setCancelQueued(context.isCancelQueued());
        trigger.setAbortRunning(context.isAbortRunning());
        return trigger;
    }
}
