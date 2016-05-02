package org.jenkinsci.plugins.github.pullrequest.trigger;

import antlr.ANTLRException;
import hudson.matrix.MatrixProject;
import hudson.model.CauseAction;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.Is.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;
import static org.junit.Assert.*;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCauseTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testCancelQueued() throws IOException, InterruptedException, ANTLRException {
        Jenkins jenkins = j.getInstance();

        FreeStyleProject project1 = jenkins.createProject(FreeStyleProject.class, "project1");

        schedule(project1, 10, "cause1_1");

        // two PR for one project in queue
        schedule(project1, 10, "cause1_2");

        //other number for project1
        schedule(project1, 12, "cause1_3");

        FreeStyleProject project2 = jenkins.createProject(FreeStyleProject.class, "project2");
        schedule(project2, 10, "project2");

        FreeStyleProject project3 = jenkins.createProject(FreeStyleProject.class, "project3");
        schedule(project3, 10, "cause1_3/project3");

        Thread.sleep(1000);
        assertThat(jenkins.getQueue().getItems(), arrayWithSize(5));


        GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.HEAVY_HOOKS, null);
        gitHubPRTrigger.setCancelQueued(true);

        JobRunnerForCause jobRunnerForCause = new JobRunnerForCause(project1, gitHubPRTrigger);

        assertThat("Should cancel something", jobRunnerForCause.cancelQueuedBuildByPrNumber(10), is(2));
        Thread.sleep(1000);

        Queue.Item[] items = jenkins.getQueue().getItems();
        assertThat(items, arrayWithSize(3));
    }

    public static QueueTaskFuture schedule(Job<?, ?> job, int number, String param) {
        ParameterizedJobMixIn jobMixIn = JobInfoHelpers.asParameterizedJobMixIn(job);
        GitHubPRCause cause = newGitHubPRCause().withNumber(number);
        ParametersAction parametersAction = new ParametersAction(
                Collections.<ParameterValue>singletonList(new StringParameterValue("value", param))
        );
        return jobMixIn.scheduleBuild2(10000, new CauseAction(cause), parametersAction);
    }
}
