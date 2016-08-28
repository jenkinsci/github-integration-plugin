package com.github.kostyasha.github.integration.generic;

import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixChildAction;
import hudson.matrix.MatrixRun;
import hudson.model.Job;
import hudson.model.Run;
import hudson.util.FlushProofOutputStream;
import jenkins.model.RunAction2;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.nonNull;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class GitHubPollingLogAction implements MatrixChildAction, RunAction2 {
    @CheckForNull
    protected transient Job<?, ?> job;

    @CheckForNull
    protected transient Run<?, ?> run;

    public GitHubPollingLogAction(Job<?, ?> job) {
        this.job = job;
    }

    public GitHubPollingLogAction(Run run) {
        this.run = run;
    }

    public abstract String getPollingFileName();

    @CheckForNull
    public Job<?, ?> getJob() {
        return job;
    }

    @CheckForNull
    public Run<?, ?> getRun() {
        return run;
    }

    public String getLog() throws IOException {
        return Util.loadFile(getPollingLogFile());
    }

    public boolean isLogExists() {
        return getPollingLogFile() != null && getPollingLogFile().isFile();
    }

    /**
     * TODO is it secure?
     */
    public void doPollingLog(StaplerRequest req, StaplerResponse rsp) throws IOException {
        rsp.setContentType("text/plain;charset=UTF-8");
        // Prevent jelly from flushing stream so Content-Length header can be added afterwards
        FlushProofOutputStream out = new FlushProofOutputStream(rsp.getCompressedOutputStream(req));
        try {
            getPollingLogText().writeLogTo(0, out);
        } finally {
            closeQuietly(out);
        }
    }

    public AnnotatedLargeText getPollingLogText() {
        return new AnnotatedLargeText<>(getPollingLogFile(), Charset.defaultCharset(), true, this);
    }

    /**
     * Writes the annotated log to the given output.
     */
    public void writePollingLogTo(XMLOutput out) throws IOException {
        new AnnotatedLargeText<>(getPollingLogFile(), Charset.defaultCharset(), true, this).writeHtmlTo(0, out.asWriter());
    }

    @Nonnull
    public File getPollingLogFile() {
        File pollingFile;
        if (nonNull(job)) {
            pollingFile = job.getRootDir();
        } else if (run instanceof MatrixRun) {
            MatrixRun matrixRun = (MatrixRun) run;
            pollingFile = matrixRun.getParentBuild().getRootDir();
        } else if (run instanceof MatrixBuild) {
            pollingFile = run.getRootDir();
        } else if (nonNull(run)) {
            pollingFile = run.getRootDir();
        } else {
            throw new IllegalStateException("Can't get polling log file: no run or job initialised");
        }

        return new File(pollingFile, getPollingFileName());
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }
}
