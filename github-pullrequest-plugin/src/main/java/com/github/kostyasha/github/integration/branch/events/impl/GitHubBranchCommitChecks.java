package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheck;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheckDescriptor;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;

import hudson.Extension;
import hudson.model.TaskListener;

import net.sf.json.JSONObject;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This branch event acts as a wrapper around checks that can be performed against commit data that requires an additional round trip to
 * GitHub to retrieve.
 * <p>
 * Commit data is retrieved and then passed to each implementing instance of <code>GitHubBranchCommitCheck</code> to determine information
 * about the commit should trigger a build.
 * </p>
 *
 * @author Kanstantsin Shautsou
 * @author Jae Gangemi
 */
public class GitHubBranchCommitChecks extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Commit Checks";

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchCommitChecks.class);

    private List<GitHubBranchCommitCheck> checks = new ArrayList<>();

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubBranchCommitChecks() {
    }

    @DataBoundConstructor
    public GitHubBranchCommitChecks(List<GitHubBranchCommitCheck> checks) {
        this.checks = checks;
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger trigger, GHBranch remoteBranch, @CheckForNull GitHubBranch localBranch,
            GitHubBranchRepository localRepo, TaskListener listener)
        throws IOException {
        if (localBranch == null) {
            LOGGER.info("First build of branch [%s] detected", remoteBranch.getName());
            return null;
        }

        GHCompare.Commit[] commits = getComparedCommits(localBranch, remoteBranch);
        List<GitHubBranchCause> causes = checks.stream()
                .map(event -> event.check(remoteBranch, localRepo, commits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (!causes.isEmpty()) {
            GitHubBranchCause cause = findFilterCause(causes);
            if (cause != null) {
                LOGGER.info("Commits for branch [{}] filtered: {}", name, cause.getReason());
            } else {
                cause = causes.get(0);
                LOGGER.info("Commits for branch [{}] allowed: {}", name, cause.getReason());
            }
            return cause;
        }

        return null;
    }

    // visible for testing to avoid complex mocking
    GHCompare.Commit[] getComparedCommits(GitHubBranch localBranch, GHBranch remoteBranch) throws IOException {
        String previous = localBranch.getCommitSha();
        String current = remoteBranch.getSHA1();

        LOGGER.debug("Comparing previous hash [{}] with current hash [{}]", previous, current);
        return remoteBranch.getOwner()
                .getCompare(previous, current)
                .getCommits();
    }

    public List<GitHubBranchCommitCheck> getChecks() {
        return checks;
    }

    void setChecks(List<GitHubBranchCommitCheck> checks) {
        this.checks = checks;
    }

    private GitHubBranchCause findFilterCause(List<GitHubBranchCause> causes) {
        GitHubBranchCause cause = causes.stream()
                .filter(GitHubBranchCause::isSkip)
                .findFirst()
                .orElse(null);

        if (cause == null) {
            return null;
        }

        LOGGER.debug("Cause indicated build should be skipped: {}", cause.getReason());
        return cause;
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);

            save();
            return super.configure(req, formData);
        }

        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }

        public List<GitHubBranchCommitCheckDescriptor> getEventDescriptors() {
            return GitHubBranchCommitCheckDescriptor.all();
        }
    }
}
