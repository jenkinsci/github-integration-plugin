package org.jenkinsci.plugins.github_integration.its;

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;
import org.kohsuke.github.GHPullRequest;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.jenkinsci.plugins.github.pullrequest.util.TestUtil.classpath;
import static org.jenkinsci.plugins.github_integration.hamcrest.CommitStatusMatcher.commitStatus;
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
        workflowJob.setQuietPeriod(10);

        workflowJob.setDefinition(
                new CpsFlowDefinition(classpath(this.getClass(), "workflowTest.groovy"))
        );
        workflowJob.save();

        super.basicTest(workflowJob);
    }

    @Test
    public void testContextStatuses() throws Exception {
        final WorkflowJob workflowJob = j.jenkins.createProject(WorkflowJob.class, "testContextStatuses");

        workflowJob.addTrigger(getPreconfiguredTrigger());
        workflowJob.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        workflowJob.setDefinition(
                new CpsFlowDefinition(classpath(this.getClass(), "testContextStatuses.groovy"))
        );
        workflowJob.save();

        super.basicTest(workflowJob);
        GHPullRequest pullRequest = ghRule.getGhRepo().getPullRequest(1);
        assertThat(pullRequest, notNullValue());

        WorkflowRun lastBuild = workflowJob.getLastBuild();
        assertThat(lastBuild, notNullValue());

        GitHubPRCause cause = lastBuild.getCause(GitHubPRCause.class);
        assertThat(cause, notNullValue());

        List<GHCommitStatus> statuses = pullRequest.getRepository().listCommitStatuses(cause.getHeadSha()).asList();
        assertThat(statuses, hasSize(7));

        // final statuses
        assertThat("workflow Run.getResult() strange",
                statuses,
                hasItem(commitStatus("testContextStatuses", GHCommitState.ERROR, "Run #1 ended normally"))
        );
        assertThat(statuses, hasItem(commitStatus("custom-context1", GHCommitState.SUCCESS, "Tests passed")));
        assertThat(statuses, hasItem(commitStatus("custom-context2", GHCommitState.SUCCESS, "Tests passed")));
        j.pause();
    }
}
