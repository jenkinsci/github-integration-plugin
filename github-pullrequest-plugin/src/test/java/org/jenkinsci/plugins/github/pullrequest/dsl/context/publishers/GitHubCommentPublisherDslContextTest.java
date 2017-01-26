package org.jenkinsci.plugins.github.pullrequest.dsl.context.publishers;

import hudson.model.Result;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GitHubCommentPublisherDslContextTest {

    private static final String COMMENT = "comment";

    private GitHubCommentPublisherDslContext context;

    @Before
    public void before() {
        context = new GitHubCommentPublisherDslContext();
    }

    @Test
    public void testPublisherErrorIsFailure() {
        whenPublisherErrorIsFailure();
        thenErrorHandlerHandlesFailures();
    }

    @Test
    public void testPublisherErrorIsUnstable() {
        whenPublisherErrorIsUnstable();
        thenErrorHandlerHandlesUnstable();
    }

    @Test
    public void testOnlyFailedBuilds() {
        whenOnlyFailedBuilds();
        thenVerifyHandlesFailed();
    }

    @Test
    public void testOnlySuccessfulBuilds() {
        whenOnlySuccessfulBuilds();
        thenVerifyHandlesSuccess();
    }

    @Test
    public void testOnlyUnstableBuilds() {
        whenOnlyUnstableBuilds();
        thenVerifyHandlesUnstable();
    }

    @Test
    public void testPublisherComment() {
        whenSetPublisherComment();
        thenPublisherCommentMatches();
    }

    private void thenErrorHandlerHandlesFailures() {
        assertThat("Publish marked as failure", context.getPublisher().getErrorHandler().getBuildStatus(), equalTo(Result.FAILURE));
    }

    private void thenErrorHandlerHandlesUnstable() {
        assertThat("Publish marked as unstable", context.getPublisher().getErrorHandler().getBuildStatus(), equalTo(Result.UNSTABLE));
    }

    private void thenPublisherCommentMatches() {
        assertThat("Comment matches", context.getPublisher().getComment().getContent(), equalTo(COMMENT));
    }

    private void thenVerifyHandlesFailed() {
        assertThat("Verifier handles failed", context.getPublisher().getStatusVerifier().getBuildStatus(), equalTo(Result.FAILURE));
    }

    private void thenVerifyHandlesSuccess() {
        assertThat("Verifier handles success", context.getPublisher().getStatusVerifier().getBuildStatus(), equalTo(Result.SUCCESS));
    }

    private void thenVerifyHandlesUnstable() {
        assertThat("Verifier handles unstable", context.getPublisher().getStatusVerifier().getBuildStatus(), equalTo(Result.UNSTABLE));
    }

    private void whenPublisherErrorIsFailure() {
        context.publisherErrorIsFailure();
    }

    private void whenPublisherErrorIsUnstable() {
        context.publisherErrorIsUnstable();
    }

    private void whenOnlyFailedBuilds() {
        context.onlyFailedBuilds();
    }

    private void whenOnlySuccessfulBuilds() {
        context.onlySuccessfulBuilds();
    }

    private void whenOnlyUnstableBuilds() {
        context.onlyUnstableBuilds();
    }

    private void whenSetPublisherComment() {
        context.comment(COMMENT);
    }
}
