package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.impl.commitchecks.GitHubBranchCommitCheck;
import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHCompare;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class GitHubBranchCommitEventTest {

    private GitHubBranchCommitEvent check;

    private GHCompare.Commit[] commits;

    @Mock
    private PrintStream logger;

    @Mock
    private GitHubBranchCause mockCause;

    @Mock
    private GHCommit mockCommit;

    @Mock
    private GitHubBranchCommitCheck mockCommitCheck;

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
        when(mockListener.getLogger()).thenReturn(logger);

        commits = new GHCompare.Commit[0];
        check = new GitHubBranchCommitEvent(Arrays.asList(mockCommitCheck)) {
            @Override
            GHCompare.Commit[] getComparedCommits(GitHubBranch localBranch, GHBranch remoteBranch) throws IOException {
                return commits;
            }

            @Override
            GHCommit getLastCommit(GHBranch remoteBranch) throws IOException {
                return mockCommit;
            }
        };
    }

    @Test
    public void testBuildIsNotTriggered() throws Exception {
        givenSkippableBranchCause();
        whenCheckCommits();
        thenCheckIsSkipped();
    }

    @Test
    public void testCommitChecksReturnNull() throws Exception {
        givenChecksReturnNull();
        whenCheckCommits();
        thenAdditionalTriggersWillBeChecked();
    }

    @Test
    public void testCommitsNotConfigured() throws Exception {
        givenNoChecksAreConfigured();
        whenCheckCommits();
        thenAdditionalTriggersWillBeChecked();
    }

    @Test
    public void testFirstSeenCommitDoesNotTriggerBuild() throws Exception {
        givenLocalRepositoryIsNull();
        givenCheckLastCommitReturnsCause();
        givenSkippableBranchCause();
        whenCheckCommits();
        thenCheckIsSkipped();
    }

    @Test
    public void testFirstSeenCommitTriggersBuild() throws Exception {
        givenLocalRepositoryIsNull();
        givenCheckLastCommitReturnsNull();
        whenCheckCommits();
        thenNoCauseReturned();
    }

    private void givenCheckLastCommitReturnsCause() {
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, mockCommit)).thenReturn(mockCause);
    }

    private void givenCheckLastCommitReturnsNull() {
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, mockCommit)).thenReturn(null);
    }

    private void givenChecksReturnNull() {
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, commits)).thenReturn(null);
    }

    private void givenLocalRepositoryIsNull() throws Exception {
        mockLocalBranch = null;
    }

    private void givenNoChecksAreConfigured() {
        check.setChecks(Collections.emptyList());
    }

    private void givenSkippableBranchCause() {
        when(mockCause.isSkip()).thenReturn(true);
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, commits)).thenReturn(mockCause);
    }

    private void thenAdditionalTriggersWillBeChecked() {
        assertNull(result);
    }

    private void thenNoCauseReturned()
    {
        assertNull("build triggered", result);
    }

    private void thenCheckIsSkipped() {
        assertThat("build triggered", result.isSkip(), is(true));
    }

    private void whenCheckCommits() throws IOException {
        result = check.check(mockTrigger, mockRemoteBranch, mockLocalBranch, mockRepo, mockListener);
    }
}
