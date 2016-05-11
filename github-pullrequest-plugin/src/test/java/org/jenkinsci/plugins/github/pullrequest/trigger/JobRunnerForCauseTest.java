package org.jenkinsci.plugins.github.pullrequest.trigger;

import hudson.Launcher;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.ACL;
import hudson.security.AuthorizationStrategy;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsArrayWithSize;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;
import static org.junit.Assert.assertThat;


/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCauseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

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

    public static QueueTaskFuture schedule(Job<?, ?> job, int number, String param, int queuetPeriod) {
        ParameterizedJobMixIn jobMixIn = JobInfoHelpers.asParameterizedJobMixIn(job);
        GitHubPRCause cause = newGitHubPRCause().withNumber(number);
        ParametersAction parametersAction = new ParametersAction(
                Collections.<ParameterValue>singletonList(new StringParameterValue("value", param))
        );
        return jobMixIn.scheduleBuild2(queuetPeriod, new CauseAction(cause), parametersAction);
    }

    public void configRoundTripUnsecure(Job job) throws Exception {
        final AuthorizationStrategy before = j.getInstance().getAuthorizationStrategy();

        j.jenkins.setAuthorizationStrategy(new AuthorizationStrategy.Unsecured());

//        j.configRoundtrip(job);

        j.getInstance().setAuthorizationStrategy(before);
    }

    @Test
    public void testAbortRunningFreestyleProject() throws Exception {

        MockFolder folder = j.createFolder("folder");

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

        MockFolder folder = j.createFolder("folder");

        WorkflowJob job1 = folder.createProject(WorkflowJob.class, "job1");
        job1.setDisplayName("Job 1 display name");
        job1.setConcurrentBuild(true);
        job1.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job1);
        job1.save();

        WorkflowJob job2 = folder.createProject(WorkflowJob.class, "job2");
        job2.setDisplayName("Job 2 display name");
        job2.setConcurrentBuild(true);
        job2.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job2);
        job2.save();

        WorkflowJob job3 = folder.createProject(WorkflowJob.class, "job3");
        job3.setDisplayName("Job 3 display name");
        job3.setConcurrentBuild(true);
        job3.setDefinition(new SleepFlow());
        configRoundTripUnsecure(job3);
        job3.save();

        testAbortRunning(job1, job2, job3);
    }

    @Test
    public void testAbortRunningMatrixProject() throws Exception {

        MockFolder folder = j.createFolder("folder");

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
    }


    private <T extends TopLevelItem> void testAbortRunning(Job<?, ?> job1, Job<?, ?> job2, Job<?, ?> job3) throws Exception {
        Jenkins jenkins = j.getInstance();

        jenkins.setNumExecutors(10);
        jenkins.save();

        jenkins.setSecurityRealm(j.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy matrixAuth = new GlobalMatrixAuthorizationStrategy();
        matrixAuth.add(Jenkins.ADMINISTER, "User");
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

        Thread.sleep(2000);

        assertThat("All runs should go to executors", jenkins.getQueue().getItems(), Matchers.<Queue.Item>emptyArray());

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should abort job1 -> number 10", job1RunnerForCause.abortRunning(10), is(2));
                assertThat("Should not abort more job1 -> number 10", job1RunnerForCause.abortRunning(10), is(0));
            }
        });

//        assertThat(jenkins.getQueue().getItems(), arrayWithSize(3));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should abort job2 -> number 10", job2RunnerForCause.abortRunning(10), is(1));
                assertThat("Should not abort more job2 -> number 10", job2RunnerForCause.abortRunning(10), is(0));
            }
        });

//        assertThat(jenkins.getQueue().getItems(), arrayWithSize(2));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should abort job3 -> number 10", job3RunnerForCause.abortRunning(10), is(1));
                assertThat("Should not abort more job3 -> number 10", job3RunnerForCause.abortRunning(10), is(0));
            }
        });

//        assertThat(jenkins.getQueue().getItems(), arrayWithSize(1));

        ACL.impersonate(Jenkins.ANONYMOUS, new Runnable() {
            @Override
            public void run() {
                assertThat("Should abort job1 -> number 12", job1RunnerForCause.abortRunning(12), is(1));
                assertThat("Should not abort more job1 -> number 12", job1RunnerForCause.abortRunning(12), is(0));
            }
        });

        assertThat(jenkins.getQueue().getItems(), IsArrayWithSize.<Queue.Item>emptyArray());
    }

    public static class SleepBuilder extends Builder {
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            TimeUnit.MINUTES.sleep(15);
            return true;
        }
    }

    public static class SleepFlow extends CpsFlowDefinition {
        public SleepFlow() {
            super("node('master') { sh 'sleep 10000 && env' }");
        }
    }
}
