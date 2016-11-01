package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.filters.GitHubBranchFilter;

import org.jenkinsci.plugins.github.pullrequest.utils.LoggingTaskListenerWrapper;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static com.github.kostyasha.github.integration.branch.trigger.check.BranchFilterToCauseConverter.toGitHubFilterCause;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchFilterToCauseConverterTest {

    @Mock
    private GHBranch mockBranch;

    @Mock
    private GitHubBranchCause mockCause;

    private List<GitHubBranchFilter> mockFilters;

    @Mock
    private GitHubBranchRepository mockLocalRepo;

    @Mock
    private LoggingTaskListenerWrapper mockPollingLog;

    @Mock
    private GitHubBranchTrigger mockTrigger;

    private GHBranch result;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFiltersDoNotMatch() throws Exception {
        givenNoFiltersMatch();
        whenFilterIsApplied();
        thenBranchIsNotFiltered();
    }

    @Test
    public void testFiltersMatch() throws Exception {
        givenAFilterMatches();
        whenFilterIsApplied();
        thenBranchIsFiltered();
    }

    @Test
    public void testNoFiltersConfigured() throws Exception {
        givenNoFiltersAreConfigured();
        whenFilterIsApplied();
        thenBranchIsNotFiltered();
    }

    private GitHubBranchFilter createMatchingFilter() throws IOException {
        GitHubBranchFilter filter = mock(GitHubBranchFilter.class);
        when(filter.check(any(), any(), any(), any(), any())).thenReturn(mockCause);

        return filter;
    }

    private GitHubBranchFilter createNonMatchingFilter() throws IOException {
        GitHubBranchFilter filter = mock(GitHubBranchFilter.class);
        when(filter.check(any(), any(), any(), any(), any())).thenReturn(mockCause);

        return filter;
    }

    private void givenAFilterMatches() throws IOException {
        when(mockCause.isSkip()).thenReturn(false).thenReturn(true);
        mockFilters = Arrays.asList(createNonMatchingFilter(), createMatchingFilter());
    }

    private void givenNoFiltersAreConfigured() {
        mockFilters = Arrays.asList();
    }

    private void givenNoFiltersMatch() throws IOException {
        mockFilters = Arrays.asList(createNonMatchingFilter(), createNonMatchingFilter());
    }

    private void thenBranchIsFiltered() {
        assertNull(result);
    }

    private void thenBranchIsNotFiltered() {
        assertNotNull(result);
        assertEquals(mockBranch, result);
    }

    private void whenFilterIsApplied() {
        when(mockTrigger.getFilters()).thenReturn(mockFilters);
        result = toGitHubFilterCause(mockLocalRepo, mockPollingLog, mockTrigger).apply(mockBranch);
    }
}
