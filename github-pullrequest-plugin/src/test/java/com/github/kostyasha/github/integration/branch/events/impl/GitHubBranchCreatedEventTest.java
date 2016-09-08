package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubBranchCreatedEventTest {
    @Mock
    private GitHubBranch localBranch;
    @Mock
    private GHRepository repository;

    @Mock
    private GHBranch remoteBranch;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;

    private final GitHubBranchCreatedEvent event = new GitHubBranchCreatedEvent();


    @Test
    public void branchCreated() throws IOException {
        commonExpectations();
        assertThat(
                event.check(null, remoteBranch, null, listener),
                notNullValue()
        );
    }

    @Test
    public void branchNotChangedExisted() throws IOException {
        commonExpectations();
        assertThat(
                event.check(null, remoteBranch, localBranch, listener),
                nullValue()
        );
    }


    @Test
    public void branchNotChangedNotExisted() throws IOException {
        commonExpectations();
        assertThat(
                event.check(null, null, null, listener),
                nullValue()
        );
    }


    @Test
    public void branchDeleted() throws IOException {
        commonExpectations();
        assertThat(
                event.check(null, null, localBranch, listener),
                nullValue()
        );
    }


    private void commonExpectations() throws IOException {
        when(localBranch.getName()).thenReturn("branch-2");

        when(remoteBranch.getName()).thenReturn("branch-1");
        when(remoteBranch.getOwner()).thenReturn(repository);

        when(listener.getLogger()).thenReturn(logger);
        when(repository.getHtmlUrl()).thenReturn(new URL("http://github.com/user/repo/"));
    }
}