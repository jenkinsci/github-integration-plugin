package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import com.github.kostyasha.github.integration.generic.GitHubRepositoryFactory;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Action;
import hudson.model.Job;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.PRHelperFunctions.asFullRepoName;

/**
 * @author Kanstantsin Shautsou
 */
@Extension
public class GitHubPRRepositoryFactory extends GitHubRepositoryFactory<GitHubPRRepositoryFactory, GitHubPRTrigger> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepositoryFactory.class);

    @Override
    public Collection<? extends Action> createFor(@Nonnull Job job) {
        try {
            if (nonNull(JobInfoHelpers.triggerFrom(job, GitHubPRTrigger.class))) {
                return Collections.singleton(forProject(job));
            }
        } catch (Exception ex) {
            LOGGER.warn("Bad configured project {} - {}", job.getFullName(), ex.getMessage());
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    @Nonnull
    private static GitHubPRRepository forProject(Job<?, ?> job) {
        XmlFile configFile = new XmlFile(new File(job.getRootDir(), GitHubPRRepository.FILE));

        GitHubPRTrigger trigger = JobInfoHelpers.triggerFrom(job, GitHubPRTrigger.class);
        GitHubRepositoryName repo = trigger.getRepoFullName(job);

        GithubProjectProperty property = job.getProperty(GithubProjectProperty.class);
        String githubUrl = property.getProjectUrl().toString();
        GitHubPRRepository localRepository;
        if (configFile.exists()) {
            try {
                localRepository = (GitHubPRRepository) configFile.read();
            } catch (IOException e) {
                LOGGER.info("Can't read saved repository, creating new one", e);
                localRepository = new GitHubPRRepository(asFullRepoName(repo), githubUrl,
                        new HashMap<Integer, GitHubPRPullRequest>());
            }
        } else {
            localRepository = new GitHubPRRepository(asFullRepoName(repo), githubUrl,
                    new HashMap<Integer, GitHubPRPullRequest>());
        }

        localRepository.setJob(job);
        localRepository.setConfigFile(configFile);
        return localRepository;
    }

    @Override
    public Class<Job> type() {
        return Job.class;
    }
}
