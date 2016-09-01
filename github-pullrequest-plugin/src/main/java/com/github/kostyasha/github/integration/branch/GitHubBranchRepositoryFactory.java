package com.github.kostyasha.github.integration.branch;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.generic.GitHubRepositoryFactory;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.asFullRepoName;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubBranchRepositoryFactory
        extends GitHubRepositoryFactory<GitHubBranchRepositoryFactory, GitHubBranchTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchRepositoryFactory.class);

    @Nonnull
    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        try {
            if (nonNull(ghBranchTriggerFromJob(job))) {
                return Collections.singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.warn("Bad configured project {} - {}", job.getFullName(), ex.getMessage());
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubBranchRepository forProject(Job<?, ?> job) {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubBranchRepository.FILE));

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
        GitHubRepositoryName repo = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubBranchRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubBranchRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                localRepository = new GitHubBranchRepository(asFullRepoName(repo), githubUrl,
                        new HashMap<String, GitHubBranch>());
            }
        } else {
            localRepository = new GitHubBranchRepository(asFullRepoName(repo), githubUrl,
                    new HashMap<String, GitHubBranch>());
        }

        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);
        return localRepository;
    }

}
