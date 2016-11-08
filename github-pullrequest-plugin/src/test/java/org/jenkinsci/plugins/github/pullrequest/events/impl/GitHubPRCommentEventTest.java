package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRCommentEventTest {

    private static final String MERGE = "merge";
    private static final String REVIEWED = "reviewed";
    private static final String LOCALLY_TESTED = "locally tested";

    @Mock
    private GHPullRequest remotePr;
    @Mock
    private GitHubPRPullRequest localPR;
    @Mock
    private GitHubPRLabel labels;
    @Mock
    private GHRepository repository;
    @Mock
    private GHIssue issue;
    @Mock
    private GHLabel mergeLabel;
    @Mock
    private GHLabel reviewedLabel;
    @Mock
    private GHLabel testLabel;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;

    @Mock
    private GitHubPRTrigger trigger;
    @Mock
    private GHIssueComment comment;

    @Test
    public void testNullLocalComment() throws IOException {
        when(listener.getLogger()).thenReturn(logger);

        when(issue.getCreatedAt()).thenReturn(new Date());
        when(comment.getBody()).thenReturn("body");

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("Comment").check(trigger, remotePr, localPR, listener);

        assertNull(cause);
    }

    @Test
    public void testNullLocalCommentRemoteMatch() throws IOException {
        commonExpectations(emptySet());
        causeCreationExpectations();

        when(issue.getCreatedAt()).thenReturn(new Date());
        when(comment.getCreatedAt()).thenReturn(new Date());
        when(comment.getBody()).thenReturn("body");

        final ArrayList<GHIssueComment> ghIssueComments = new ArrayList<>();
        ghIssueComments.add(comment);
        when(remotePr.getComments()).thenReturn(ghIssueComments);

        GitHubPRCause cause = new GitHubPRCommentEvent("body").check(trigger, remotePr, localPR, listener);

        assertNotNull(cause);
    }

    @Test
    public void testNoComments() throws IOException {
        when(remotePr.getComments()).thenReturn(emptyList());

        GitHubPRCause cause = new GitHubPRCommentEvent("Comment").check(null, remotePr, localPR, listener);

        assertNull(cause);
    }

    @Test
    public void testNullLocalPR() {
        GitHubPRCause cause = new GitHubPRCommentEvent("").check(null, null, null, listener);

        assertNull(cause);
    }

    private void commonExpectations(Set<String> localLabels) throws IOException {
        when(labels.getLabelsSet()).thenReturn(localLabels);
        when(localPR.getLabels()).thenReturn(localLabels);
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePr.getRepository()).thenReturn(repository);
        when(repository.getIssue(anyInt())).thenReturn(issue);
        when(repository.getOwnerName()).thenReturn("ownerName");
        when(listener.getLogger()).thenReturn(logger);
    }

    private void causeCreationExpectations() throws IOException {
        GHUser mockUser = mock(GHUser.class);
        GHCommitPointer mockPointer = mock(GHCommitPointer.class);

        when(remotePr.getUser()).thenReturn(mockUser);
        when(remotePr.getHead()).thenReturn(mockPointer);
        when(remotePr.getBase()).thenReturn(mockPointer);
    }
}