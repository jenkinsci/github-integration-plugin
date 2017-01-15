package com.github.kostyasha.github.integration.branch.dsl.context.events;

import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCreatedEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchDeletedEvent;
import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchHashChangedEvent;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchEventsDslContext implements Context {

    private List<GitHubBranchEvent> events = new ArrayList<>();

    public void branchRestriction(Runnable closure) {
        GitHubBranchFilterEventDslContext filterContext = new GitHubBranchFilterEventDslContext();
        ContextExtensionPoint.executeInContext(closure, filterContext);

        events.add(filterContext.getFilter());
    }

    public void commitChecks(Runnable closure) {
        GitHubBranchCommitChecksDslContext checksContext = new GitHubBranchCommitChecksDslContext();
        ContextExtensionPoint.executeInContext(closure, checksContext);

        events.add(checksContext.getCheck());
    }

    public void created() {
        events.add(new GitHubBranchCreatedEvent());
    }

    public void deleted() {
        events.add(new GitHubBranchDeletedEvent());
    }

    public void hashChanged() {
        events.add(new GitHubBranchHashChangedEvent());
    }

    public List<GitHubBranchEvent> events() {
        return events;
    }
}
