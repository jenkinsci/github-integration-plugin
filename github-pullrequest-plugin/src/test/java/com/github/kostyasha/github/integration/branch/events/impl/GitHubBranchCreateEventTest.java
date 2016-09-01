package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author Kanstantsin Shautsou
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubBranchCreateEventTest {
    @Mock
    private GitHubBranch localBranch;
    @Mock
    private GHRepository repository;
    @Mock
    private TaskListener listener;
    @Mock
    private PrintStream logger;


    /**
     * Case when there is three checked labels and there is one that was added and one that already exists.
     */
    @Test
    public void secondOfThreeLabelsWasAdded() throws IOException {

    }
}