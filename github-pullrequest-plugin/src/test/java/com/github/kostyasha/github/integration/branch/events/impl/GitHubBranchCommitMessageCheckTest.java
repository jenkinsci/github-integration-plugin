package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare.Commit;
import org.kohsuke.github.GHCompare.InnerCommit;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class GitHubBranchCommitMessageCheckTest {

    private static final String PATTERN = ".*\\[maven-release-plugin\\].*";

    private GitHubBranchCommitMessageCheck event;

    @Mock
    private Commit mockCommit;

    private Commit[] mockCommits;

    @Mock
    private InnerCommit mockInnerCommit;

    @Mock
    private GitHubBranch mockLocalBranch;

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchRepository mockRepo;

    private GitHubBranchCause result;

    @Before
    public void begin() {
        MockitoAnnotations.initMocks(this);
        event = new GitHubBranchCommitMessageCheck();
    }

    @Test
    public void testExcludeCommit() {
        givenACommitMessage();
        givenAMessagePattern();
        givenMessageShouldBeExcluded();
        whenEventIsChecked();
        thenEventIsSkipped();
    }

    @Test
    public void testIncludeCommit() {
        givenACommitMessage();
        givenAMessagePattern();
        whenEventIsChecked();
        thenEventIsTriggered();
    }

    @Test
    public void testNoPatternsSpecified() {
        givenACommitMessage();
        whenEventIsChecked();
        thenEventIsTriggered();
    }

    private void givenACommitMessage() {
        when(mockInnerCommit.getMessage()).thenReturn("commit message");
        when(mockInnerCommit.getMessage()).thenReturn("[maven-release-plugin] commit message");

        when(mockCommit.getCommit()).thenReturn(mockInnerCommit);

        mockCommits = new Commit[] { mockCommit, mockCommit };
    }

    private void givenAMessagePattern()
    {
        event.setMatchCriteria(PATTERN);
    }

    private void givenMessageShouldBeExcluded() {
        event.setExclude(true);
    }

    private void thenEventIsSkipped() {
        assertThat("build triggered", result.isSkip(), is(true));
    }

    private void thenEventIsTriggered() {
        assertThat("build triggered", result.isSkip(), is(false));
    }

    private void whenEventIsChecked() {
        result = event.check(mockRemoteBranch, mockRepo, mockCommits);
    }
}
