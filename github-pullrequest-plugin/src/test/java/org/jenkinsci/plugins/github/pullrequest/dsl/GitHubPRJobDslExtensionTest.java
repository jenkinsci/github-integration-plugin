package org.jenkinsci.plugins.github.pullrequest.dsl;

import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.LookupStrategy;
import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.RemovedViewAction;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS_CRON;
import static org.junit.Assert.*;

/**
 * @author Kanstantsin Shautsou
 */
public class GitHubPRJobDslExtensionTest {

    public static final String JOB_NAME_IN_DSL_SCRIPT = "gh-branch";
    public static final String JOB_DSL_GROOVY = "dsl/branch-jobdsl.groovy";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldCreateJobWithExtendedDsl() throws Exception {
        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.getBuildersList().add(
                new ExecuteDslScripts(
                        new ExecuteDslScripts.ScriptLocation(
                                null, null,
                                IOUtils.toString(this
                                        .getClass().getClassLoader().getResourceAsStream(JOB_DSL_GROOVY))
                        ),
                        false,
                        RemovedJobAction.DELETE,
                        RemovedViewAction.DELETE,
                        LookupStrategy.JENKINS_ROOT
                )
        );

        jenkins.buildAndAssertSuccess(job);

        assertThat(jenkins.getInstance().getJobNames(), hasItem(is(JOB_NAME_IN_DSL_SCRIPT)));

        FreeStyleProject generated = jenkins.getInstance()
                .getItemByFullName(JOB_NAME_IN_DSL_SCRIPT, FreeStyleProject.class);


        Collection<Trigger<?>> triggers = generated.getTriggers().values();
        assertThat("Should add trigger", triggers, hasSize(1));
        GitHubBranchTrigger trigger = (GitHubBranchTrigger) triggers.toArray()[0];
        assertThat("Should add trigger of GHPR class", trigger, instanceOf(GitHubBranchTrigger.class));
        assertThat("Should have pre status", trigger.isPreStatus(), equalTo(true));
        assertThat("Should have cancel queued", trigger.isCancelQueued(), equalTo(true));
        assertThat("Should add events", trigger.getEvents(), hasSize(3));
        assertThat("Should set mode", trigger.getTriggerMode(), equalTo(HEAVY_HOOKS_CRON));
    }

}