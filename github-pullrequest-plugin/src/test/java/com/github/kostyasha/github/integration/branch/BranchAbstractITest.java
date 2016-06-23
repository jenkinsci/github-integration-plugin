package com.github.kostyasha.github.integration.branch;

import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import org.jenkinsci.plugins.github_integration.junit.GHRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.JenkinsRule;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;

/**
 * @author Kanstantsin Shautsou
 */
public class BranchAbstractITest {

    public static JenkinsRule j = new JenkinsRule();

    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static GHRule ghRule = new GHRule(j, temporaryFolder);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(temporaryFolder)
            .around(j)
            .around(ghRule);


    public void basicBranchTest(Job<?, ?> job) throws Exception {
        // fails with workflow
        if (job instanceof FreeStyleProject || job instanceof MatrixProject) {
            j.configRoundtrip(job); // activate trigger
        }

        GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);

//        runTriggerAndWaitUntilEnd(trigger, 10 * GH_API_DELAY);


    }


}
