package org.jenkinsci.plugins.github.pullrequest.dsl.context.publishers;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.kohsuke.github.GHCommitState;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPRStatusPublisherDslContext implements Context {

    private GitHubPRMessage message;
    private GHCommitState state;

    public void message(String content) {
        message = new GitHubPRMessage(content);
    }

    public void unstableAsError() {
        this.state = GHCommitState.ERROR;
    }

    public GHCommitState unstableAs() {
        return state;
    }

    public GitHubPRMessage message() {
        return message;
    }
}
