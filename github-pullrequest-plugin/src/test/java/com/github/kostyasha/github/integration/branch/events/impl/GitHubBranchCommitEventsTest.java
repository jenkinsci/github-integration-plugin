package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.kohsuke.github.GHCompare.Commit;

public class GitHubBranchCommitEventsTest {

    private static final String PATTERN = ".*\\[maven-release-plugin\\].*";

    private GitHubBranchCommitMessageEvent event;

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchRepository mockRepo;

    @Mock
    private GitHubBranch mockLocalBranch;


    private Commit[] mockCommits;

    private GitHubBranchCause result;

    @Before
    public void begin() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNoPatternsSpecified() {
    }

    @Test
    public void testIncludeCommit() {
        givenACommitMessage();
        whenEventIsChecked();
        thenEventIsTriggered();
    }

    private void thenEventIsTriggered() {
        // TODO Auto-generated method stub

    }

    private void whenEventIsChecked() {
        //result = event.check(mockRemoteBranch, mockLocalBranch, mockCommits);

    }

    @Test
    public void testExcludeCommit() {
        givenACommitMessage();
        givenMessageShouldBeExcluded();
        whenEventIsChecked();
        thenEventIsSkipped();
    }

    private void thenEventIsSkipped() {
        // TODO Auto-generated method stub

    }

    private void givenMessageShouldBeExcluded() {
        // TODO Auto-generated method stub

    }

    private void givenACommitMessage() {
        // TODO Auto-generated method stub

    }
}
