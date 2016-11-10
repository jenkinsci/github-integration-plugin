package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheck;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class GitHubBranchCommitChecksTest {

    private GHCompare.Commit[] commits;

    private GitHubBranchCommitChecks event;

    @Mock
    private GitHubBranchCause mockCause;

    @Mock
    private GitHubBranchCommitCheck mockCommitEvent;

    @Mock
    private LoggingTaskListenerWrapper mockListener;

    @Mock
    private GitHubBranch mockLocalBranch;

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchRepository mockRepo;

    @Mock
    private GitHubBranchTrigger mockTrigger;

    private GitHubBranchCause result;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        commits = new GHCompare.Commit[0];
        event = new GitHubBranchCommitChecks(Arrays.asList(mockCommitEvent)) {
            @Override
            GHCompare.Commit[] getComparedCommits(GitHubBranch localBranch, GHBranch remoteBranch) throws IOException {
                return commits;
            }
        };
    }

    @Test
    public void testBuildIsNotTriggered() throws Exception {
        givenSkippableBranchCause();
        whenCheckCommits();
        thenEventIsSkipped();
    }

    @Test
    public void testBuildIsTriggered() throws Exception {
        givenBuildableBranchCause();
        whenCheckCommits();
        thenEventIsTriggered();
    }

    @Test
    public void testEventChecksReturnNull() throws Exception {
        givenEventsReturnNull();
        whenCheckCommits();
        thenAdditionalTriggersWillBeChecked();
    }

    @Test
    public void testEventsNoConfigured() throws Exception {
        givenNoEventsAreConfigured();
        whenCheckCommits();
        thenAdditionalTriggersWillBeChecked();
    }

    @Test
    public void testFirstCommit() throws Exception {
        givenTheFirstCommit();
        whenCheckCommits();
        thenNoCauseReturned();
    }

    private void givenBuildableBranchCause() {
        when(mockCause.isSkip()).thenReturn(false);
        when(mockCommitEvent.check(mockRemoteBranch, mockRepo, commits)).thenReturn(mockCause);
    }

    private void givenEventsReturnNull() {
        when(mockCommitEvent.check(mockRemoteBranch, mockRepo, commits)).thenReturn(null);
    }

    private void givenNoEventsAreConfigured() {
        event.setEvents(Collections.emptyList());
    }

    private void givenSkippableBranchCause() {
        when(mockCause.isSkip()).thenReturn(true);
        when(mockCommitEvent.check(mockRemoteBranch, mockRepo, commits)).thenReturn(mockCause);
    }

    private void givenTheFirstCommit() throws Exception {
        mockLocalBranch = null;
    }

    private void thenAdditionalTriggersWillBeChecked() {
        assertNull(result);
    }

    private void thenNoCauseReturned()
    {
        assertNull("build triggered", result);
    }

    private void thenEventIsSkipped() {
        assertThat("build triggered", result.isSkip(), is(true));
    }

    private void thenEventIsTriggered() {
        assertThat("build triggered", result.isSkip(), is(false));
    }

    private void whenCheckCommits() throws IOException {
        result = event.check(mockTrigger, mockRemoteBranch, mockLocalBranch, mockRepo, mockListener);
    }
}
