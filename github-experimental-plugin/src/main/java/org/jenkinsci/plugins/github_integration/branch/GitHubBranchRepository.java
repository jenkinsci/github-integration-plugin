package org.jenkinsci.plugins.github_integration.branch;

import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchRepository implements Action, Saveable {
    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    public static final String FILE = GitHubBranchRepository.class.getName() + ".runtime.xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchRepository.class);
    private transient XmlFile configFile; // for save()
    private transient Job<?, ?> job;  // for UI

    private final String fullName;
    private final String githubUrl;

    private Map<String, GitHubLocalBranch> branches;

    /**
     * Object that represent GitHub repository to work with
     *
     * @param fullName repository full name. for case of changed jobs url
     * @param branches previous branches states
     */
    public GitHubBranchRepository(String fullName, String githubUrl, Map<String, GitHubLocalBranch> branches) {
        this.fullName = fullName;
        this.githubUrl = githubUrl;
        this.branches = branches;
    }

    public Map<String, GitHubLocalBranch> getBranches() {
        return branches;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR";
    }

    @Override
    public String getUrlName() {
        return "github-pullrequest";
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    @Override
    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }

        configFile.write(this);
        SaveableListener.fireOnChange(this, configFile);
    }

    public void saveQuetly() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.error("Can't save repository state, because: '{}'", e.getMessage(), e);
        }
    }


    public void setJob(Job<?, ?> job) {
        this.job = job;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }

}
