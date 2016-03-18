package org.jenkinsci.plugins.github.pullrequest.dsl.context.events;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCloseEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommentEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRDescriptionEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelAddedEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelNotExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelPatternExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelRemovedEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNonMergeableEvent;
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

    public void commented(String comment) {
        events.add(new GitHubPRCommentEvent(comment));
    }

    public void skipDescription(String skipMsg) {
        events.add(new GitHubPRDescriptionEvent(skipMsg));
    }

    public void labelAdded(String label) { new GitHubPRLabelAddedEvent(new GitHubPRLabel(label)); }

    public void labelExists(String label) { new GitHubPRLabelExistsEvent(new GitHubPRLabel(label), false); }

    public void skipLabelExists(String label) { new GitHubPRLabelExistsEvent(new GitHubPRLabel(label), true); }

    public void labelNotExists(String label) { new GitHubPRLabelNotExistsEvent(new GitHubPRLabel(label), false); }

    public void skipLabelNotExists(String label) { new GitHubPRLabelNotExistsEvent(new GitHubPRLabel(label), true); }

    public void labelsMatchPattern(String pattern) { new GitHubPRLabelPatternExistsEvent(new GitHubPRLabel(pattern), false); }

    public void labelsMatchPatternSkip(String pattern) { new GitHubPRLabelPatternExistsEvent(new GitHubPRLabel(pattern), true); }

    public void labelRemoved(String label) { new GitHubPRLabelRemovedEvent(new GitHubPRLabel(label)); }

    public void nonMergeable() { new GitHubPRNonMergeableEvent(false); }

    public void skipNonMergeable() { new GitHubPRNonMergeableEvent(true); }

    public List<GitHubPREvent> events() {
        return events;
    }
}
