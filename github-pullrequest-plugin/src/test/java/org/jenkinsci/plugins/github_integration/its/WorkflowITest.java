package org.jenkinsci.plugins.github_integration.its;

import antlr.ANTLRException;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.github.GHPullRequest;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jenkinsci.plugins.github.pullrequest.util.TestUtil.classpath;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredTrigger;


/**
 * @author Kanstantsin Shautsou
 */
public class WorkflowITest extends AbstractPRTest {

    @Test
    public void workflowTest() throws Exception {
        final WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "it-job");

        workflowJob.addTrigger(getPreconfiguredTrigger());
        workflowJob.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        workflowJob.setDefinition(
                new CpsFlowDefinition(classpath(this.getClass(), "workflow-definition.groovy"))
        );
        workflowJob.save();

//        j.pause();

        super.basicTest(workflowJob);
//        j.pause();
    }

    @Test
    public void testContextStatuses() throws Exception {
        final WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "workflow-job-statuses");

        workflowJob.addTrigger(getPreconfiguredTrigger());
        workflowJob.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        workflowJob.setDefinition(
                new CpsFlowDefinition(classpath(this.getClass(), "workflow-definition2.groovy"))
        );
        workflowJob.save();

        super.basicTest(workflowJob);
        j.pause();
        GHPullRequest pullRequest = ghRule.getGhRepo().getPullRequest(1);
        assertThat(pullRequest, notNullValue());
    }
}
