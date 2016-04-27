package org.jenkinsci.plugins.github_integration.its;

import antlr.ANTLRException;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.matrix.TextAxis;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage;
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;
import org.junit.Test;

import java.io.IOException;

import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredProperty;
import static org.jenkinsci.plugins.github_integration.junit.GHRule.getPreconfiguredTrigger;

/**
 * @author Kanstantsin Shautsou
 */
public class MatrixProjectITest extends AbstractPRTest {

    @Test
    public void testChildStatuses() throws Exception {
        final MatrixProject matrixProject = j.jenkins.createProject(MatrixProject.class, "matrix-project");

        matrixProject.addTrigger(getPreconfiguredTrigger());
        matrixProject.addProperty(getPreconfiguredProperty(ghRule.getGhRepo()));

        matrixProject.getBuildersList().add(new GitHubPRStatusBuilder());
        matrixProject.getBuildersList().add(new Shell("sleep 10"));

        matrixProject.getPublishersList().add(new GitHubPRBuildStatusPublisher());
        matrixProject.getPublishersList().add(new GitHubPRCommentPublisher(new GitHubPRMessage("Comment"), null, null));


        matrixProject.setAxes(
                new AxisList(
                        new TextAxis("first_axis", "first_value1", "first_value2"),
                        new TextAxis("second_axis", "sec_value1", "sec_value2")
                )
        );

        matrixProject.save();

        super.basicTest(matrixProject);

    }
}
