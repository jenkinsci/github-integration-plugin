package org.jenkinsci.plugins.github_integration.its;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github_integration.junit.GHRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHPullRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.GH_API_DELAY;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.runTriggerAndWaitUntilEnd;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.waitUntilPRAppears;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractPRTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractPRTest.class);

    public static JenkinsRule j = new JenkinsRule();

    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static GHRule ghRule = new GHRule(j, temporaryFolder);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder)
            .around(j)
            .around(ghRule);

    public void basicTest(Job job) throws Exception {
        // fails with workflow
        if (job instanceof FreeStyleProject) {
            j.configRoundtrip(job); // activate trigger
        }

        // update trigger (maybe useless)
        GitHubPRTrigger trigger = ghPRTriggerFromJob(job);
//        trigger.start(job, true); // hack configRountrip that doesn't work with workflow

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        j.waitUntilNoActivity();

        assertThat(job.getLastBuild(), is(nullValue()));

        GitHubPRRepository ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, notNullValue());

        Map<Integer, GitHubPRPullRequest> pulls = ghPRRepository.getPulls();
        assertThat("Action storage should be empty", pulls.entrySet(), Matchers.hasSize(0));

        final GHPullRequest pullRequest1 = ghRule.getGhRepo().createPullRequest("title", "branch-1", "master", "body");
        waitUntilPRAppears(pullRequest1, 60 * GH_API_DELAY); // GH API slow, wait longer

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        j.waitUntilNoActivity();

        // refresh objects
        ghPRRepository = job.getAction(GitHubPRRepository.class);
        assertThat("Action storage should be available", ghPRRepository, notNullValue());

        pulls = ghPRRepository.getPulls();
        assertThat("Pull request 1 should appear in action storage", pulls.entrySet(), Matchers.hasSize(1));

        j.assertBuildStatusSuccess(job.getLastBuild());
    }
}
