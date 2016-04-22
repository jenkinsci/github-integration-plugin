package org.jenkinsci.plugins.github_integration.junit;

import antlr.ANTLRException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.Job;
import hudson.util.Secret;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;

/**
 * @author Kanstantsin Shautsou
 */
public class GHRule implements TestRule {
    private static final Logger LOG = LoggerFactory.getLogger(GHRule.class);

    public static final String GH_TOKEN = System.getenv("GH_TOKEN");
    public static final long GH_API_DELAY = 1000;

    private GHRepository ghRepo;
    private GitHub gitHub;
    private Git git;

    @CheckForNull
    private GitHubServerConfig gitHubServerConfig;

    @Nonnull
    private static JenkinsRule jRule;

    @Nonnull
    private static TemporaryFolder temporaryFolder;

    public GHRule(JenkinsRule jRule, TemporaryFolder temporaryFolder) {
        this.jRule = jRule;
        this.temporaryFolder = temporaryFolder;
    }

    /**
     * github connection from configured github-plugin from global configuration.
     */
    public GitHub getGitHub() {
        return gitHub;
    }

    /**
     * GitHub repository that was created in {@link #before}.
     */
    public GHRepository getGhRepo() {
        return ghRepo;
    }


    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before(description);
//                try {
                base.evaluate();
//                } finally {
//                    after();
//                }
            }
        };
    }

    @Before
    public void before(Description description) throws IOException, GitAPIException, URISyntaxException, InterruptedException {
        String repoName = description.getClassName() + "-" + description.getMethodName();
        assertThat("Specify GH_TOKEN variable", GH_TOKEN, notNullValue());

        //reuse github client for GitHub preparation
        gitHubServerConfig = prepareGitHubPlugin();
        //FIXME no idea why github-plugin doesn't find configuration without delay
        Thread.sleep(15000);
        gitHub = loginToGithub().apply(gitHubServerConfig);
        assertThat("Specify right GH_TOKEN variable!", gitHub, notNullValue());
        LOG.debug(gitHub.getRateLimit().toString());

        ghRepo = gitHub.getMyself().getRepository(repoName);
        if (ghRepo != null) {
            LOG.info("Deleting {}", ghRepo.getHtmlUrl());
            ghRepo.delete();
        }

        ghRepo = gitHub.createRepository(repoName, "", "", true);
        LOG.info("Created {}", ghRepo.getHtmlUrl());

        // prepare git
        final File gitRootDir = temporaryFolder.newFolder();

        git = Git.init().setDirectory(gitRootDir).call();

        writeStringToFile(new File(gitRootDir, "README.md"), "Test repo");
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage("Initial commit").call();

        final RefSpec refSpec = new RefSpec("+refs/heads/*:refs/remotes/origin/*");

        final StoredConfig storedConfig = git.getRepository().getConfig();
        final RemoteConfig origin = new RemoteConfig(storedConfig, "origin");
        origin.addURI(new URIish(ghRepo.gitHttpTransportUrl()));
        origin.addPushRefSpec(refSpec);
        origin.update(storedConfig);
        storedConfig.save();


        git.branchCreate().setName("branch-1").call();
        git.checkout().setName("branch-1").call();
        commitFile(gitRootDir, "branch-1.file", "content", "commit for branch-1");

        git.branchCreate().setName("branch-2").call();
        git.checkout().setName("branch-2").call();
        commitFile(gitRootDir, "branch-2.file", "content", "commit for branch-2");

        git.checkout().setName("master").call();

        git.push()
                .setPushAll()
                .setProgressMonitor(new TextProgressMonitor())
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(GH_TOKEN, ""))
                .call();
    }

    /**
     * Prepare global GitHub plugin configuration.
     * Nothing specific to job.
     */
    public static GitHubServerConfig prepareGitHubPlugin() {
        // prepare global jRule settings
        final StringCredentialsImpl cred = new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                null,
                "description",
                Secret.fromString(GH_TOKEN)
        );

        SystemCredentialsProvider.getInstance().getCredentials().add(cred);

        final GitHubPluginConfig gitHubPluginConfig = GitHubPlugin.configuration();

        final List<GitHubServerConfig> gitHubServerConfigs = new ArrayList<>();
        final GitHubServerConfig gitHubServerConfig = new GitHubServerConfig(cred.getId());
        gitHubServerConfig.setManageHooks(false);
        gitHubServerConfigs.add(gitHubServerConfig);

        gitHubPluginConfig.setConfigs(gitHubServerConfigs);

        return gitHubServerConfig;
    }

    public static GitHubPRTrigger getPreconfiguredTrigger() throws ANTLRException {
        final ArrayList<GitHubPREvent> gitHubPREvents = new ArrayList<>();
        gitHubPREvents.add(new GitHubPROpenEvent());
        gitHubPREvents.add(new GitHubPRCommitEvent());

        final GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.CRON, gitHubPREvents);
        gitHubPRTrigger.setPreStatus(true);

        return gitHubPRTrigger;
    }

    public static GithubProjectProperty getPreconfiguredProperty(GHRepository ghRepo) {
        return new GithubProjectProperty(ghRepo.getHtmlUrl().toString());
    }

    public void commitFile(File gitRootDir, String fileName, String content, String commitMessage)
            throws IOException, GitAPIException {
        writeStringToFile(new File(gitRootDir, fileName), commitMessage);
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage(commitMessage).call();
    }

    public static void waitUntilPRAppears(GHPullRequest pullRequest, long timeout) throws InterruptedException {
        final GHRepository repository = pullRequest.getRepository();
        long startTime = System.currentTimeMillis();

        while (true) {
            Thread.sleep(2 * 1000);
            for (GHPullRequest pr : repository.listPullRequests(GHIssueState.OPEN).asList()) {
                if (pr.getId() == pullRequest.getId()) {
                    LOG.debug("Delay : {}", System.currentTimeMillis() - startTime);
                    return;
                }
            }

            if (System.currentTimeMillis() - startTime > timeout) {
                throw new AssertionError("PR " + pullRequest + " doesn't appear in list of PRs");
            }
        }
    }

    public static void runTriggerAndWaitUntilEnd(GitHubPRTrigger trigger, long timeout)
            throws InterruptedException, IOException {
        final Job<?, ?> job = trigger.getJob();
        Objects.requireNonNull(job, "Job must exist in trigger, initialise trigger!");

        String oldLog = null;
        final GitHubPRPollingLogAction oldAction = job.getAction(GitHubPRPollingLogAction.class);
        if (oldAction != null) {
            oldLog = oldAction.getLog();
        }

        trigger.doRun(null);

        long startTime = System.currentTimeMillis();
        while (true) {
            Thread.sleep(10);
            final GitHubPRPollingLogAction prLogAction = job.getAction(GitHubPRPollingLogAction.class);
            try {
                if (prLogAction != null) {
                    final String newLog = prLogAction.getLog();
                    if (!newLog.equals(oldLog) && newLog.contains(GitHubPRTrigger.FINISH_MSG)) {
                        return;
                    }
                }
            } catch (IOException ignore) {
            }

            if (System.currentTimeMillis() - startTime > timeout) {
                throw new AssertionError("Trigger didn't started or finished");
            }
        }
    }
}
