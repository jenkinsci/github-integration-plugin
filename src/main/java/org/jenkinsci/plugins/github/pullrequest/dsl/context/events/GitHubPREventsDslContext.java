package org.jenkinsci.plugins.github.pullrequest.dsl.context.events;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCloseEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPREventsDslContext implements Context {
    private List<GitHubPREvent> events = new ArrayList<>();

    public void closed() {
        events.add(new GitHubPRCloseEvent());
    }

    public void opened() {
        events.add(new GitHubPROpenEvent());
    }

    public void commit() {
        events.add(new GitHubPRCommitEvent());
    }

    public List<GitHubPREvent> events() {
        return events;
    }
}
