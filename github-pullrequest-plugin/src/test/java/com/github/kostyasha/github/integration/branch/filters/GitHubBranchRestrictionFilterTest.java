package com.github.kostyasha.github.integration.branch.filters;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.filters.GitHubBranchRestrictionFilter;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class GitHubBranchRestrictionFilterTest {

    private static final String BRANCH1 = "branch1";
    private static final String BRANCH2 = "branch2";
    private static final String BRANCH3 = "3branch";

    private static final String MASTER = "master";
    private static final String OTHER = "other";

    private static final String PATTERN1 = "\\w+\\d+";
    private static final String PATTERN2 = "\\d+\\w+";

    private GitHubBranchRestrictionFilter filter;

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchRepository mockRepo;

    @Mock
    private GitHubBranch mockLocalBranch;

    @Mock
    private GitHubBranchTrigger mockTrigger;

    @Mock
    private LoggingTaskListenerWrapper mockPollingLog;

    private GitHubBranchCause result;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        filter = new GitHubBranchRestrictionFilter();
    }

    @Test
    public void testBranchNameExclude() {
        givenMatchAsExact();
        givenRestrictionsShouldExclude();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(OTHER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();
    }

    @Test
    public void testBranchNameInclude() {
        givenMatchAsExact();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(OTHER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();
    }

    @Test
    public void testBranchNamePatternExclude() {
        givenMatchAsPatterns();
        givenRestrictionsShouldExclude();

        givenRemoteBranchName(BRANCH1);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(BRANCH3);
        whenApplyBranchFilter();
        thenBranchWasFiltered();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();
    }

    @Test
    public void testBranchNamePatternInclude() {
        givenMatchAsPatterns();

        givenRemoteBranchName(BRANCH1);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(BRANCH3);
        whenApplyBranchFilter();
        thenBranchWasNotFiltered();

        givenRemoteBranchName(MASTER);
        whenApplyBranchFilter();
        thenBranchWasFiltered();
    }

    private String createTestInput(String... strings) {
        return String.join(System.lineSeparator(), strings);
    }

    private void givenMatchAsExact() {
        filter.setMatchCriteria(createTestInput(MASTER, BRANCH1, BRANCH2, BRANCH3));
    }

    private void givenMatchAsPatterns() {
        filter.setMatchAsPattern(true);
        filter.setMatchCriteria(createTestInput(PATTERN1, PATTERN2));
    }

    private void givenRemoteBranchName(String name) {
        when(mockRemoteBranch.getName()).thenReturn(name);
    }

    private void givenRestrictionsShouldExclude() {
        filter.setExclude(true);
    }

    private void thenBranchWasFiltered() {
        assertTrue("buildable filtered", result.isSkip());
    }

    private void thenBranchWasNotFiltered() {
        assertFalse("buildable not filtered", result.isSkip());
    }

    private void whenApplyBranchFilter() {
        result = filter.check(mockTrigger, mockRemoteBranch, mockLocalBranch, mockRepo, mockPollingLog);
    }
}
