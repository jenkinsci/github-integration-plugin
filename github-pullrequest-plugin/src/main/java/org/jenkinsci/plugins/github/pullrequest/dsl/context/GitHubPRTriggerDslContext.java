package org.jenkinsci.plugins.github.pullrequest.dsl.context;

import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.dsl.context.events.GitHubPREventsDslContext;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPRTriggerDslContext implements Context {
    private String cron = "H/5 * * * *";
    private GitHubPRTriggerMode mode = GitHubPRTriggerMode.CRON;
    private boolean setPreStatus;
    private boolean cancelQueued;
    private List<GitHubPREvent> events = new ArrayList<>();

    public void cron(String cron) {
        this.cron = cron;
    }

    public void mode(Runnable closure) {
        GitHubPRTriggerModeDslContext modeContext = new GitHubPRTriggerModeDslContext();
        ContextExtensionPoint.executeInContext(closure, modeContext);

        mode = modeContext.mode();
    }

    public void setPreStatus() {
        setPreStatus = true;
    }

    public void cancelQueued() {
        cancelQueued = true;
    }

    public void events(Runnable closure) {
        GitHubPREventsDslContext eventsContext = new GitHubPREventsDslContext();
        ContextExtensionPoint.executeInContext(closure, eventsContext);

        events.addAll(eventsContext.events());
    }

    public String cron() {
        return cron;
    }

    public GitHubPRTriggerMode mode() {
        return mode;
    }

    public boolean isSetPreStatus() {
        return setPreStatus;
    }

    public boolean isCancelQueued() {
        return cancelQueued;
    }


    public List<GitHubPREvent> events() {
        return events;
    }
}
