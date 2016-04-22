package org.jenkinsci.plugins.github_integration.its;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kanstantsin Shautsou
 */
public class WorkflowContextStatusITest {
    private static final Logger LOG = LoggerFactory.getLogger(WorkflowContextStatusITest.class);

    protected static final String JOB_NAME = "it-job";

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testContextStatuses() {

    }
}
