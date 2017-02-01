package org.jenkinsci.plugins.github.pullrequest.dsl.context.publishers;

import hudson.model.Result;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;
import org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;

import javaposse.jobdsl.dsl.Context;

public class GitHubCommentPublisherDslContext implements Context {

    public static final GitHubPRCommentPublisher DEFAULT_PUBLISHER = new GitHubPRCommentPublisher();

    private PublisherErrorHandler errorHandler;

    private GitHubPRMessage message = GitHubPRCommentPublisher.DEFAULT_COMMENT;

    private StatusVerifier verifier;

    public void message(String comment) {
        message = new GitHubPRMessage(comment);
    }

    public GitHubPRCommentPublisher getPublisher() {
        return new GitHubPRCommentPublisher(message, verifier, errorHandler);
    }

    public void publisherErrorIsFailure() {
        errorHandler = new PublisherErrorHandler(Result.FAILURE);
    }

    public void publisherErrorIsUnstable() {
        errorHandler = new PublisherErrorHandler(Result.UNSTABLE);
    }

    public void onlyFailedBuilds() {
        verifier = new StatusVerifier(Result.FAILURE);
    }

    public void onlySuccessfulBuilds() {
        verifier = new StatusVerifier(Result.SUCCESS);
    }

    public void onlyUnstableBuilds() {
        verifier = new StatusVerifier(Result.UNSTABLE);
    }
}
