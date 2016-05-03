package org.jenkinsci.plugins.github.pullrequest.trigger;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import hudson.security.AuthorizationStrategy;
import hudson.tasks.Builder;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.jenkinsci.plugins.github.pullrequest.GitHubPRCause.newGitHubPRCause;

/**
 * @author Kanstantsin Shautsou
 */
public class JobRunnerForCauseTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

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
