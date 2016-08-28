package com.github.kostyasha.github.integration.branch;

import hudson.matrix.MatrixProject;
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
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHPullRequest;

import java.util.Map;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRTriggerFromJob;
import static org.jenkinsci.plugins.github_integration.awaitility.GHBranchAppeared.ghBranchAppeared;
import static org.jenkinsci.plugins.github_integration.awaitility.GHPRAppeared.ghPRAppeared;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.GH_API_DELAY;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.runTriggerAndWaitUntilEnd;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchITest {

    public static JenkinsRule jRule = new JenkinsRule();

    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static GHRule ghRule = new GHRule(jRule, temporaryFolder);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder)
            .around(jRule)
            .around(ghRule);


    public void smokeTest(Job<?, ?> job) throws Exception {
        // fails with workflow
        if (job instanceof FreeStyleProject || job instanceof MatrixProject) {
            jRule.configRoundtrip(job); // activate trigger
        }

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);

//        trigger.start(job, true); // hack configRountrip that doesn't work with workflow

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        jRule.waitUntilNoActivity();

        assertThat(job.getLastBuild(), is(nullValue()));

        GitHubBranchRepository ghRepository = job.getAction(GitHubBranchRepository.class);
        assertThat("Action storage should be available", ghRepository, notNullValue());

        Map<String, GitHubLocalBranch> branches = ghRepository.getBranches();

        assertThat("Action storage should be empty", branches.entrySet(), Matchers.hasSize(0));

        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);

        jRule.waitUntilNoActivity();

        // refresh objects
        ghRepository = job.getAction(GitHubBranchRepository.class);
        assertThat("Action storage should be available", ghRepository, notNullValue());

        branches = ghRepository.getBranches();
        assertThat("Pull request 1 should appear in action storage", branches.entrySet(), Matchers.hasSize(1));

        jRule.assertBuildStatusSuccess(job.getLastBuild());
    }

}
