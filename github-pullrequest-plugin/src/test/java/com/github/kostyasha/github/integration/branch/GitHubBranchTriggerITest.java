package com.github.kostyasha.github.integration.branch;

import hudson.model.FreeStyleProject;
import hudson.tasks.Shell;
import org.junit.Test;

import static org.jenkinsci.plugins.github_integration.junit.GHRule.getDefaultBranchTrigger;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerITest extends BranchITest {
    @Test
    public void freestyleTest() throws Exception {
        // create job
        FreeStyleProject job = jRule.createFreeStyleProject("freestyle-job");

        job.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        job.addTrigger(getDefaultBranchTrigger());

        job.getBuildersList().add(new Shell("sleep 10"));

        job.save();

        super.smokeTest(job);
    }
}