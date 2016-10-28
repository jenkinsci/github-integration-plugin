package com.github.kostyasha.github.integration.branch.trigger;

import static com.github.kostyasha.github.integration.branch.trigger.SkipFirstRunForBranchFilter.skipFirstBuildFilter;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SkipFirstBuildableRunFilterTest {

    @Mock
    private GitHubBranch mockLocalBranch;

    @Mock
    private LoggingTaskListenerWrapper mockLogger;

    @Mock
    private GHBranch mockRemoteBranch;

    @Mock
    private GitHubBranchRepository mockRepo;

    private boolean result;
    private boolean skipFirstRun;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFilterDisabledAndFirstBuild() {
        givenFirstBuildableRun();
        whenFilterBuildable();
        thenBuildableWasNotFiltered();
    }

    @Test
    public void testFilterDisabledAndNextBuild() {
        givenNextBuildableRun();
        whenFilterBuildable();
        thenBuildableWasNotFiltered();
    }

    @Test
    public void testFilterEnabledAndFirstBuild() {
        givenSkipFirstRun();
        givenFirstBuildableRun();
        whenFilterBuildable();
        thenBuildableWasFiltered();
    }

    @Test
    public void testFilterEnabledAndNextBuild() {
        givenSkipFirstRun();
        givenNextBuildableRun();
        whenFilterBuildable();
        thenBuildableWasNotFiltered();
    }

    private void givenFirstBuildableRun() {
        when(mockRepo.getBranch(any())).thenReturn(null);
    }

    private void givenNextBuildableRun() {
        when(mockRepo.getBranch(any())).thenReturn(mockLocalBranch);
    }

    private void givenSkipFirstRun() {
        skipFirstRun = true;
    }

    private void thenBuildableWasFiltered() {
        assertFalse("buildable filtered", result);
    }

    private void thenBuildableWasNotFiltered() {
        assertTrue("buildable filtered", result);
    }

    private void whenFilterBuildable() {
        result = skipFirstBuildFilter(skipFirstRun, mockRepo, mockLogger).test(mockRemoteBranch);
    }
}
