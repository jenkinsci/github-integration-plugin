package com.github.kostyasha.github.integration.branch.trigger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GitHubBranchFilterChainTest {

    private GitHubBranchFilterChain delegate;

    @Mock
    private GHBranch mockBranch;

    private boolean result;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        delegate = GitHubBranchFilterChain.filterChain();
    }

    @Test
    public void testFilterIsApplied() {
        givenADelegateThatWillReturnTrue();
        whenFilterBuildable();
        thenFilterWasApplied();
    }

    @Test
    public void testFilterIsNotApplied() {
        givenADelegateThatWillReturnFalse();
        whenFilterBuildable();
        thenFilterWasNotApplied();
    }

    private Predicate<GHBranch> createFalseFilter() {
        Predicate<GHBranch> filter = mock(Predicate.class);
        when(filter.test(any())).thenReturn(false);

        return filter;
    }

    private Predicate<GHBranch> createTrueFilter() {
        Predicate<GHBranch> filter = mock(Predicate.class);
        when(filter.test(any())).thenReturn(true);

        return filter;
    }

    private void givenADelegateThatWillReturnFalse() {
        delegate.with(createFalseFilter()).with(createFalseFilter());
    }

    private void givenADelegateThatWillReturnTrue() {
        delegate.with(createFalseFilter()).with(createTrueFilter());
    }

    private void thenFilterWasApplied() {
        assertTrue("filter applied", result);
    }

    private void thenFilterWasNotApplied() {
        assertFalse("filter applied", result);
    }

    private void whenFilterBuildable() {
        result = delegate.test(mockBranch);
    }
}
