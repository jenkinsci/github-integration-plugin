package com.github.kostyasha.github.integration.branch.trigger;

import static com.github.kostyasha.github.integration.branch.trigger.BranchRepositoryUpdater.branchRepoUpdater;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class BranchRepositoryUpdaterTest {

    @Mock
    private GHBranch mockBranch;

    @Mock
    private GitHubBranchRepository mockBranchRepo;

    @Mock
    private GHRepository mockRepo;

    private BranchRepositoryUpdater updater;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        updater = branchRepoUpdater(mockBranchRepo);
    }

    @Test
    public void testUpdateLocalBranches() throws Exception {
        givenAGHBranch();
        whenUpdateRepository();
        thenBranchWasAdded();
    }

    private void givenAGHBranch() throws MalformedURLException {
        when(mockRepo.getHtmlUrl()).thenReturn(new URL("http://example.com"));

        when(mockBranch.getName()).thenReturn("branch");
        when(mockBranch.getOwner()).thenReturn(mockRepo);
    }

    private void thenBranchWasAdded() {
        verify(mockBranchRepo).addBranch(any());
    }

    private void whenUpdateRepository() {
        updater.apply(mockBranch);
    }
}
