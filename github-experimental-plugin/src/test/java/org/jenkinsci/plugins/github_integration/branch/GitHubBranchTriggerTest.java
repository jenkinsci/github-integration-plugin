package org.jenkinsci.plugins.github_integration.branch;

import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.junit.Test;
import org.jvnet.hudson.test.MockFolder;

import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchTriggerTest extends BranchAbstractITest {

    @Test
    public void testRepo() throws Exception {
        final MockFolder folder = j.createFolder("folder");
        FreeStyleProject job = folder.createProject(FreeStyleProject.class, "freestyle-job");

        final GitHubBranchTrigger branchTrigger = new GitHubBranchTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS);

        job.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));
        job.addTrigger(branchTrigger);

        super.basicBranchTest(job);
    }
}