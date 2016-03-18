package org.jenkinsci.plugins.github.pullrequest.dsl.context;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPRTriggerModeDslContext implements Context {
    private GitHubPRTriggerMode mode = GitHubPRTriggerMode.CRON;

    public void cron() {
        mode = GitHubPRTriggerMode.CRON;
    }

    public void heavyHooks() {
        mode = GitHubPRTriggerMode.HEAVY_HOOKS;
    }

    public void heavyHooksCron() {
        mode = GitHubPRTriggerMode.HEAVY_HOOKS_CRON;
    }

    public GitHubPRTriggerMode mode() {
        return mode;
    }

}
