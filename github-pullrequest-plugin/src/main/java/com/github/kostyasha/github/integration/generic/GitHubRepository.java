package com.github.kostyasha.github.integration.generic;

import com.cloudbees.jenkins.GitHubWebHook;
import hudson.BulkChange;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubRepository<T extends GitHubRepository> implements Action, Saveable {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubRepository.class);

    protected transient XmlFile configFile; // for save()
    protected transient Job<?, ?> job;  // for UI

    private final String fullName;
    private final String githubUrl;


    public GitHubRepository(String fullName, String githubUrl) {
        this.fullName = fullName;
        this.githubUrl = githubUrl;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setJob(Job<?, ?> job) {
        this.job = job;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }

    public void saveQuietly() {
        try {
            save();
        } catch (IOException e) {
            LOG.error("Can't save repository state, because: '{}'", e.getMessage(), e);
        }
    }

    @Override
    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }

        configFile.write(this);
        SaveableListener.fireOnChange(this, configFile);
    }

    public abstract FormValidation doClearRepo() throws IOException;

    public abstract FormValidation doRunTrigger() throws IOException;

    public abstract FormValidation doRebuildFailed() throws IOException;

    public abstract FormValidation doRebuild(StaplerRequest req) throws IOException;

}
