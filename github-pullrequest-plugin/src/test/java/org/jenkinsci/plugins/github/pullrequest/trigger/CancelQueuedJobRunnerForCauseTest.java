package org.jenkinsci.plugins.github.pullrequest.trigger;

import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.hamcrest.collection.IsArrayWithSize;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.jvnet.hudson.test.MockFolder;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class CancelQueuedJobRunnerForCauseTest extends JobRunnerForCauseTest {

    @Test
    public void testCancelQueuedFreestyleProject() throws Exception {
        cancelQueued(FreeStyleProject.class);
    }

    @Test
    public void testCancelQueuedWorkflowJob() throws Exception {
        cancelQueued(WorkflowJob.class);
    }

    @Test
    public void testCancelQueuedMatrixProject() throws Exception {
        cancelQueued(MatrixProject.class);
    }

    private <T extends TopLevelItem> void cancelQueued(Class<T> tClass) throws Exception {
        Jenkins jenkins = j.getInstance();

        GlobalMatrixAuthorizationStrategy matrixAuth = new GlobalMatrixAuthorizationStrategy();
        matrixAuth.add(Jenkins.ADMINISTER, "User");
        jenkins.setAuthorizationStrategy(matrixAuth);

        MockFolder folder = j.createFolder("folder");

        Job job1 = (Job) folder.createProject(tClass, "project1");
        job1.setDisplayName("project1 display name");

        configRoundTripUnsecure(job1);

        job1.save();

        final GitHubPRTrigger gitHubPRTrigger1 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger1.setCancelQueued(true);
        gitHubPRTrigger1.start(job1, true); // to have working polling log files
        final JobRunnerForCause job1RunnerForCause = new JobRunnerForCause(job1, gitHubPRTrigger1);

        schedule(job1, 10, "cause1_1", 10000);

        // two PR for one project in queue
        schedule(job1, 10, "cause1_2", 10000);

        //other number for project1
        schedule(job1, 12, "cause1_3", 10000);

        Job job2 = (Job) folder.createProject(tClass, "project2");
        job2.setDisplayName("project2 displayName");
        job2.save();
        configRoundTripUnsecure(job2);
        schedule(job2, 10, "job2", 10000);

        final GitHubPRTrigger gitHubPRTrigger2 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger2.setCancelQueued(true);
        gitHubPRTrigger2.start(job2, true); // to have working polling log files
        final JobRunnerForCause job2RunnerForCause = new JobRunnerForCause(job2, gitHubPRTrigger2);


        Job job3 = (Job) folder.createProject(tClass, "project3");
        job3.setDisplayName("project3 displayName");
        job3.save();
        configRoundTripUnsecure(job3);
        schedule(job3, 10, "cause1_3/project3", 10000);

        final GitHubPRTrigger gitHubPRTrigger3 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger3.setCancelQueued(true);
        gitHubPRTrigger3.start(job3, true); // to have working polling log files
        final JobRunnerForCause job3RunnerForCause = new JobRunnerForCause(job3, gitHubPRTrigger3);


        Thread.sleep(1000);
        assertThat(jenkins.getQueue().getItems(), arrayWithSize(5));


        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job1 -> number 10", job1RunnerForCause.cancelQueuedBuildByPrNumber(10), is(2));
                assertThat("Should not cancel more job1 -> number 10", job1RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(3));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job2 -> number 10", job2RunnerForCause.cancelQueuedBuildByPrNumber(10), is(1));
                assertThat("Should not cancel more job2 -> number 10", job2RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(2));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job3 -> number 10", job3RunnerForCause.cancelQueuedBuildByPrNumber(10), is(1));
                assertThat("Should not cancel more job3 -> number 10", job3RunnerForCause.cancelQueuedBuildByPrNumber(10), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), arrayWithSize(1));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should cancel job1 -> number 12", job1RunnerForCause.cancelQueuedBuildByPrNumber(12), is(1));
                assertThat("Should not cancel more job1 -> number 12", job1RunnerForCause.cancelQueuedBuildByPrNumber(12), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), IsArrayWithSize.<Queue.Item>emptyArray());
    }

}
