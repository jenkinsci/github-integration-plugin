package com.github.kostyasha.github.integration.branch.trigger.check;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;

import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URL;
import java.util.Map;

import static com.github.kostyasha.github.integration.branch.trigger.check.LocalRepoUpdater.updateLocalRepo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalRepoUpdaterTest {

    @Mock
    private GHBranch mockBranch;

    @Mock
    private GitHubBranchRepository mockBranchRepo;

    @Mock
    private GHRepository mockRepo;

    @Mock
    private Map<String, GitHubBranch> mockMap;

    private LocalRepoUpdater updater;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        updater = updateLocalRepo(mockBranchRepo);
    }

    @Test
    public void testUpdateLocalBranches() throws Exception {
        givenAGHBranch();
        whenUpdateRepository();
        thenBranchWasAdded();
    }

    private void givenAGHBranch() throws Exception {
        when(mockRepo.getHtmlUrl()).thenReturn(new URL("http://example.com"));

        when(mockBranch.getName()).thenReturn("branch");
        when(mockBranch.getOwner()).thenReturn(mockRepo);

        when(mockBranchRepo.getBranches()).thenReturn(mockMap);
    }

    private void thenBranchWasAdded() {
        verify(mockBranchRepo).getBranches();
        verify(mockMap).put(any(), any());
    }

    private void whenUpdateRepository() {
        updater.apply(mockBranch);
    }
}
