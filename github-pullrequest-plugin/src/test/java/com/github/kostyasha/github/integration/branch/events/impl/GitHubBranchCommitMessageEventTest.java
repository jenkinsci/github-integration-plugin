package com.github.kostyasha.github.integration.branch.events.impl;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class GitHubBranchCommitMessageEventTest {

    @Before
    public void begin() {
        MockitoAnnotations.initMocks(this);
    }




    private String createTestInput(String... strings) {
        return String.join(System.lineSeparator(), strings);
    }

}
