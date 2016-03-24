package org.jenkinsci.plugins.github_integration.its;

import antlr.ANTLRException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.coravy.hudson.plugins.github.GithubProjectProperty;
import hudson.model.FreeStyleProject;
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
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCheckName;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsNot.not;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.mockito.Matchers.isNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractPRTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPRTest.class);

    public static final String GH_TOKEN = System.getenv("GH_TOKEN");
    public static final long GH_API_DELAY = 1000;
    protected static final String JOB_NAME = "it-job";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    protected GHRepository ghRepo;
    protected GitHub gitHub;
    protected Git git;

    @Before
    public void before() throws IOException, GitAPIException, URISyntaxException {
        // prepare global jenkins settings
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

        //reuse github client for GitHub preparation

        gitHub = loginToGithub().apply(gitHubServerConfig);
        assertThat("Specify GH_TOKEN variable", gitHub, notNullValue());
        LOG.debug(gitHub.getRateLimit().toString());

        ghRepo = gitHub.getMyself().getRepository(getClass().getName());
        if (ghRepo != null) {
            LOG.info("Deleting {}", ghRepo.getHtmlUrl());
            ghRepo.delete();
        }

        ghRepo = gitHub.createRepository(getClass().getName(), "", "", true);
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

    public void basicTest() throws Exception {
        final Job<?, ?> job = (Job<?, ?>) j.getInstance().getItem(JOB_NAME);

        // fails with workflow
        if (job instanceof FreeStyleProject) {
            j.configRoundtrip(job); // activate trigger
        }

        // update trigger (maybe useless)
        GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
//        trigger.start(job, true); // hack configRountrip that doesn't work with workflow

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        j.waitUntilNoActivity();

        assertThat(job.getLastBuild(), is(isNull()));

        GitHubPRRepository ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, not(isNull()));

        Map<Integer, GitHubPRPullRequest> pulls = ghPRRepository.getPulls();
        assertThat("Action storage should be empty", pulls.entrySet(), Matchers.hasSize(0));

        final GHPullRequest pullRequest1 = ghRepo.createPullRequest("title", "branch-1", "master", "body");
        waitUntilPRAppears(pullRequest1, 60 * GH_API_DELAY); // GH API slow, wait longer

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        j.waitUntilNoActivity();

        // refresh objects
        ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, not(isNull()));

        pulls = ghPRRepository.getPulls();
        assertThat("Pull request 1 should appear in action storage", pulls.entrySet(), Matchers.hasSize(1));

        j.assertBuildStatusSuccess(job.getLastBuild());
    }

    public static GitHubPRTrigger getPreconfiguredTrigger() throws ANTLRException {
        final ArrayList<GitHubPREvent> gitHubPREvents = new ArrayList<>();
        gitHubPREvents.add(new GitHubPROpenEvent());
        gitHubPREvents.add(new GitHubPRCommitEvent());

        final GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.CRON, gitHubPREvents);
        gitHubPRTrigger.setPreStatus(new GitHubPRCheckName(""));

        return gitHubPRTrigger;
    }

    public GithubProjectProperty getPreconfiguredProperty() {
        return new GithubProjectProperty(ghRepo.getHtmlUrl().toString());
    }

    protected void commitFile(File gitRootDir, String fileName, String content, String commitMessage)
            throws IOException, GitAPIException {
        writeStringToFile(new File(gitRootDir, fileName), commitMessage);
        git.add().addFilepattern(".").call();
        git.commit().setAll(true).setMessage(commitMessage).call();
    }

    protected static void waitUntilPRAppears(GHPullRequest pullRequest, long timeout) throws InterruptedException {
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

    protected static void runTriggerAndWaitUntilEnd(GitHubPRTrigger trigger, long timeout)
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
