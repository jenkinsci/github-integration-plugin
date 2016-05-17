package org.jenkinsci.plugins.github.pullrequest.trigger;

import com.google.common.base.Throwables;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsArrayWithSize;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.jvnet.hudson.test.MockFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Kanstantsin Shautsou
 */
public class AbortRunningJobRunnerCauseTest extends JobRunnerForCauseTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbortRunningJobRunnerCauseTest.class);

    @Test
    public void testAbortRunningFreestyleProject() throws Exception {

        MockFolder folder = j.createFolder("freestyle_folder");

        FreeStyleProject job1 = folder.createProject(FreeStyleProject.class, "project1");
        job1.setDisplayName("project1 display name");
        job1.setConcurrentBuild(true);
        job1.getBuildersList().add(new SleepBuilder());
        configRoundTripUnsecure(job1);
        job1.save();

        FreeStyleProject job2 = folder.createProject(FreeStyleProject.class, "project2");
        job2.setDisplayName("project1 display name");
        job2.setConcurrentBuild(true);
        job2.getBuildersList().add(new SleepBuilder());
        configRoundTripUnsecure(job2);
        job2.save();

        FreeStyleProject job3 = folder.createProject(FreeStyleProject.class, "project3");
        job3.setDisplayName("project1 display name");
        job3.setConcurrentBuild(true);
        job3.getBuildersList().add(new SleepBuilder());
        configRoundTripUnsecure(job3);
        job3.save();

        testAbortRunning(job1, job2, job3);
    }

    @Test
    public void testAbortRunningWorkflow() throws Exception {

        MockFolder folder = j.createFolder("workflow_folder");

        WorkflowJob job1 = folder.createProject(WorkflowJob.class, "job1");
        job1.setDisplayName("WorkflowJob 1 display name");
        job1.setConcurrentBuild(true);
        job1.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job1);
        job1.save();

        WorkflowJob job2 = folder.createProject(WorkflowJob.class, "job2");
        job2.setDisplayName("WorkflowJob 2 display name");
        job2.setConcurrentBuild(true);
        job2.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job2);
        job2.save();

        WorkflowJob job3 = folder.createProject(WorkflowJob.class, "job3");
        job3.setDisplayName("WorkflowJob 3 display name");
        job3.setConcurrentBuild(true);
        job3.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job3);
        job3.save();

        Thread.sleep(15 * 1000); // allow get jobs to executors
        testAbortRunning(job1, job2, job3);
    }

    @Test
    public void testAbortRunningMatrixProject() throws Exception {

        MockFolder folder = j.createFolder("Matrix_folder");

        MatrixProject job1 = folder.createProject(MatrixProject.class, "project1");
        job1.setDisplayName("project1 display name");
        job1.setConcurrentBuild(true);
        job1.getBuildersList().add(new SleepBuilder());
        job1.setAxes(
                new AxisList(
                        new TextAxis("first_axis", "first_value1"),
                        new TextAxis("second_axis", "sec_value1")
                )
        );
        configRoundTripUnsecure(job1);
        job1.save();

        MatrixProject job2 = folder.createProject(MatrixProject.class, "project2");
        job2.setDisplayName("project1 display name");
        job2.setConcurrentBuild(true);
        job2.getBuildersList().add(new SleepBuilder());
        job2.setAxes(
                new AxisList(
                        new TextAxis("first_axis", "first_value1"),
                        new TextAxis("second_axis", "sec_value1")
                )
        );
        configRoundTripUnsecure(job2);
        job2.save();

        MatrixProject job3 = folder.createProject(MatrixProject.class, "project3");
        job3.setDisplayName("project1 display name");
        job3.setConcurrentBuild(true);
        job3.getBuildersList().add(new SleepBuilder());
        job3.setAxes(
                new AxisList(
                        new TextAxis("first_axis", "first_value1"),
                        new TextAxis("second_axis", "sec_value1")
                )
        );
        configRoundTripUnsecure(job3);
        job3.save();

        testAbortRunning(job1, job2, job3);

        assertThat(job1.getBuilds(), hasSize(3));

        for (MatrixBuild matrixBuild : job1.getBuilds()) {
            assertThat(matrixBuild.getResult(), is(Result.ABORTED));
            assertThat(matrixBuild.getRuns(), not(empty()));
            for (MatrixRun matrixRun : matrixBuild.getRuns()) {
                assertThat(matrixRun.getResult(), is(Result.ABORTED));
            }
        }
    }

    public <T extends TopLevelItem> void testAbortRunning(Job<?, ?> job1, Job<?, ?> job2, Job<?, ?> job3) throws Exception {
        final Jenkins jenkins = j.getInstance();

        jenkins.setNumExecutors(10);
        jenkins.save();

        jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy matrixAuth = new GlobalMatrixAuthorizationStrategy();
        matrixAuth.add(Jenkins.ADMINISTER, "a");
        jenkins.setAuthorizationStrategy(matrixAuth);
        jenkins.save();

        schedule(job1, 10, "cause1_1", 0);
        // two PR for one project in queue
        schedule(job1, 10, "cause1_2", 0);
        //other number for project1
        schedule(job1, 12, "cause1_3", 0);

        schedule(job2, 10, "job2", 0);

        schedule(job3, 10, "cause1_3/project3", 0);

        final GitHubPRTrigger gitHubPRTrigger1 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger1.setAbortRunning(true);
        gitHubPRTrigger1.start(job1, true); // to have working polling log files
        final JobRunnerForCause job1RunnerForCause = new JobRunnerForCause(job1, gitHubPRTrigger1);


        final GitHubPRTrigger gitHubPRTrigger2 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger2.setAbortRunning(true);
        gitHubPRTrigger2.start(job2, true); // to have working polling log files
        final JobRunnerForCause job2RunnerForCause = new JobRunnerForCause(job2, gitHubPRTrigger2);

        final GitHubPRTrigger gitHubPRTrigger3 = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger3.setAbortRunning(true);
        gitHubPRTrigger3.start(job3, true); // to have working polling log files
        final JobRunnerForCause job3RunnerForCause = new JobRunnerForCause(job3, gitHubPRTrigger3);

        await().timeout(50, TimeUnit.SECONDS).until(new Runnable() {
            @Override
            public void run() {
                assertThat(jenkins.getQueue().getItems(), emptyArray());
            }
        });

        assertThat("All runs should go to executors", jenkins.getQueue().getItems(), Matchers.<Queue.Item>emptyArray());

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                try {
                    assertThat("Should abort job1 -> number 10", job1RunnerForCause.abortRunning(10), is(2));
                    assertThat("Should not abort more job1 -> number 10", job1RunnerForCause.abortRunning(10), is(0));
                } catch (IllegalAccessException e) {
                    Throwables.propagate(e);
                }
            }
        });

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                try {
                    assertThat("Should abort job2 -> number 10", job2RunnerForCause.abortRunning(10), is(1));
                    assertThat("Should not abort more job2 -> number 10", job2RunnerForCause.abortRunning(10), is(0));
                } catch (IllegalAccessException e) {
                    Throwables.propagate(e);
                }
            }
        });

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                try {
                    assertThat("Should abort job3 -> number 10", job3RunnerForCause.abortRunning(10), is(1));
                    assertThat("Should not abort more job3 -> number 10", job3RunnerForCause.abortRunning(10), is(0));
                } catch (IllegalAccessException e) {
                    Throwables.propagate(e);
                }
            }
        });

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                try {
                    assertThat("Should abort job1 -> number 12", job1RunnerForCause.abortRunning(12), is(1));
                    assertThat("Should not abort more job1 -> number 12", job1RunnerForCause.abortRunning(12), is(0));
                } catch (IllegalAccessException e) {
                    Throwables.propagate(e);
                }
            }
        });

        assertThat(jenkins.getQueue().getItems(), IsArrayWithSize.<Queue.Item>emptyArray());

//        final RunList<?> run1List = job1.getBuilds();
//        assertThat(run1List, not(empty()));
//        for (Run run : run1List) {
//            assertThat(run.getResult(), is(Result.ABORTED));
//        }
        j.waitUntilNoActivity();
    }
}
