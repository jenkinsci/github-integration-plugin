package com.github.kostyasha.github.integration.generic;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Optional;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import java.net.URI;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubTriggerDescriptor extends TriggerDescriptor {
    private transient SequentialExecutionQueue queue =
            new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

    @Nonnull
    public SequentialExecutionQueue getQueue() {
        if (queue == null) {
            queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
        }
        return queue;
    }

    private String publishedURL;

    public String getPublishedURL() {
        return publishedURL;
    }

    public void setPublishedURL(String publishedURL) {
        this.publishedURL = publishedURL;
    }

    public String getJenkinsURL() {
        String url = getPublishedURL();
        if (isNotBlank(url)) {
            if (!url.endsWith("/")) {
                url += "/";
            }
            return url;
        }
        return GitHubWebHook.getJenkinsInstance().getRootUrl();
    }

    @Nonnull
    public static GitHub githubFor(URI uri) {
        Optional<GitHub> client = from(GitHubPlugin.configuration()
                .findGithubConfig(withHost(uri.getHost()))).first();
        if (client.isPresent()) {
            return client.get();
        } else {
            throw new GHPluginConfigException("Can't find appropriate client for github repo <%s>", uri);
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        req.bindJSON(this, formData);

        save();
        return super.configure(req, formData);
    }

    @Override
    public boolean isApplicable(Item item) {
        return item instanceof Job && nonNull(SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(item))
                && item instanceof ParameterizedJobMixIn.ParameterizedJob;
    }

}
