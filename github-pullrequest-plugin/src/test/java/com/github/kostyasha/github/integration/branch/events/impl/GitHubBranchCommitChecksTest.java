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

    private GitHubBranchCommitChecks check;

    @Mock
    private GitHubBranchCause mockCause;

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

        commits = new GHCompare.Commit[0];
        check = new GitHubBranchCommitChecks(Arrays.asList(mockCommitCheck)) {
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
    public void testFirstCommit() throws Exception {
        givenTheFirstCommit();
        whenCheckCommits();
        thenNoCauseReturned();
    }

    private void givenChecksReturnNull() {
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, commits)).thenReturn(null);
    }

    private void givenNoChecksAreConfigured() {
        check.setChecks(Collections.emptyList());
    }

    private void givenSkippableBranchCause() {
        when(mockCause.isSkip()).thenReturn(true);
        when(mockCommitCheck.check(mockRemoteBranch, mockRepo, commits)).thenReturn(mockCause);
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

    private void thenCheckIsSkipped() {
        assertThat("build triggered", result.isSkip(), is(true));
    }

    private void whenCheckCommits() throws IOException {
        result = check.check(mockTrigger, mockRemoteBranch, mockLocalBranch, mockRepo, mockListener);
    }
}
