package org.jenkinsci.plugins.github.pullrequest.events.impl;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHLabel;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Alina Karpovich
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubPRNonMergeableEventTest {

    @Mock
    private GitHubPRTrigger trigger;
    @Mock
    private GHPullRequest remotePr;
    @Mock
    private GitHubPRPullRequest localPr;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;
    @Mock
    private GHUser ghUser;
    @Mock
    private GHCommitPointer ghCommitPointer;
    @Mock
    private GHIssue ghIssue;
    @Mock
    private GHRepository ghRepository;

    @Before
    public void before() throws IOException {
        when(remotePr.getUser()).thenReturn(ghUser);
        when(remotePr.getHead()).thenReturn(ghCommitPointer);
        when(remotePr.getBase()).thenReturn(ghCommitPointer);
        when(ghCommitPointer.getSha()).thenReturn("1r134rsha324");
        when(ghCommitPointer.getRef()).thenReturn("some/com.github.kostyasha.github.integration.branch");
        when(remotePr.getRepository()).thenReturn(ghRepository);
        when(ghRepository.getIssue(0)).thenReturn(ghIssue);
        when(ghIssue.getLabels()).thenReturn(Collections.<GHLabel>emptySet());
        when(remotePr.getState()).thenReturn(GHIssueState.OPEN);
    }

    @Test
    public void remotePRIsMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent(true);

        when(remotePr.getMergeable()).thenReturn(true);
        when(listener.getLogger()).thenReturn(logger);

        assertTrue(instance.isSkip());
        assertNull(instance.check(trigger, remotePr, localPr, listener));
    }

    @Test
    public void remotePRIsNotMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent(true);

        when(remotePr.getMergeable()).thenReturn(false);
        when(listener.getLogger()).thenReturn(logger);
        when(ghUser.getLogin()).thenReturn("user");


        assertNotNull(instance.check(trigger, remotePr, localPr, listener));
        assertTrue(instance.isSkip());
    }

    @Test
    public void remotePRReturnsNullForMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent(true);

        when(remotePr.getMergeable()).thenReturn(null);
        when(listener.getLogger()).thenReturn(logger);

        assertTrue(instance.isSkip());
        assertNotNull(instance.check(trigger, remotePr, localPr, listener));
    }

    @Test
    public void remotePRThrowsExceptionForMergeable() throws IOException {
        GitHubPRNonMergeableEvent instance = new GitHubPRNonMergeableEvent(true);

        when(remotePr.getMergeable()).thenThrow(new IOException("test IO"));
        when(listener.getLogger()).thenReturn(logger);

        assertNotNull(instance.check(trigger, remotePr, localPr, listener));
    }
}
