package org.jenkinsci.plugins.github.pullrequest;

import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.Job;
import org.apache.commons.jelly.XMLOutput;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Link for project page, shows triggered polling log
 *
 * @author Alina Karpovich
 */
public class GitHubPRPollingLogAction implements Action {
    private transient Job<?, ?> project;

    public GitHubPRPollingLogAction(Job<?, ?> project) {
        this.project = project;
    }

    public Job<?, ?> getOwner() {
        return project;
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR Polling Log";
    }

    @Override
    public String getUrlName() {
        return "github-pr-polling";
    }

    public String getLog() throws IOException {
        return Util.loadFile(getLogFile());
    }

    public String getPollingFileName() {
        return "github-pullrequest-polling.log";
    }

    /**
     * Writes the annotated log to the given output.
     *
     * @since 1.350
     */
    public void writeLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<>(getLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
    }

    public File getLogFile() {
        return new File(project.getRootDir(), getPollingFileName());
    }
}
