package org.jenkinsci.plugins.github_integration.its;

import antlr.ANTLRException;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredTrigger;

/**
 * @author Kanstantsin Shautsou
 */
public class MatrixProjectITest extends AbstractPRTest {

    @Test
    public void testChildStatuses() throws Exception {
        final MatrixProject matrixProject = j.jenkins.createProject(MatrixProject.class, "matrix-project");

        matrixProject.setAxes(
                new AxisList(
                        new TextAxis("first_axis", "first_value1", "first_value2"),
                        new TextAxis("second_axis", "sec_value1", "sec_value2")
                )
        );

        final ArrayList<GitHubPREvent> gitHubPREvents = new ArrayList<>();
        gitHubPREvents.add(new GitHubPROpenEvent());
        gitHubPREvents.add(new GitHubPRCommitEvent());

        final GitHubPRTrigger gitHubPRTrigger = new GitHubPRTrigger("", GitHubPRTriggerMode.CRON, gitHubPREvents);

        matrixProject.addTrigger(gitHubPRTrigger);
        matrixProject.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        matrixProject.getBuildersList().add(new GitHubPRStatusBuilder());
        matrixProject.getBuildersList().add(new Shell("sleep 10"));

        matrixProject.getPublishersList().add(new GitHubPRBuildStatusPublisher());
        matrixProject.getPublishersList().add(new GitHubPRCommentPublisher(new GitHubPRMessage("Comment"), null, null));

        matrixProject.save();

        super.basicTest(matrixProject);
    }
}
