package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitEventDescriptor;
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
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchCommitEvents extends GitHubBranchEvent {
    private static final String DISPLAY_NAME = "Commit Events";

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchCommitEvents.class);

    private List<GitHubBranchCommitEvent> events = new ArrayList<>();

    /**
     * For groovy UI
     */
    @Restricted(value = NoExternalUse.class)
    public GitHubBranchCommitEvents() {
    }

    @DataBoundConstructor
    public GitHubBranchCommitEvents(List<GitHubBranchCommitEvent> events) {
        this.events = events;
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger trigger, GHBranch remoteBranch, @CheckForNull GitHubBranch localBranch,
            GitHubBranchRepository localRepo, TaskListener listener)
        throws IOException {
        if (localBranch == null) {
            return toCause(remoteBranch, localRepo, false, "First build of branch [%s]", remoteBranch.getName());
        }

        String previous = localBranch.getCommitSha();
        String current = remoteBranch.getSHA1();

        LOGGER.debug("comparing previous hash [{}] with current hash [{}]", previous, current);
        GHCompare.Commit[] commits = remoteBranch.getOwner()
                .getCompare(previous, current)
                .getCommits();

        List<GitHubBranchCause> causes = events.stream()
                .map(event -> event.check(remoteBranch, localRepo, commits))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String name = remoteBranch.getName();
        if (!causes.isEmpty()) {
            GitHubBranchCause cause = findFilterCause(causes);
            if (cause != null) {
                LOGGER.info("Commits for branch [{}] filtered: {}", name, cause.getReason());
                return cause;
            }
        }

        GitHubBranchCause cause = causes.get(0);
        LOGGER.info("Commits for branch [{}] allowed: {}", name, cause.getReason());

        return cause;
    }

    public List<GitHubBranchCommitEvent> getEvents() {
        return events;
    }

    public void setEvents(List<GitHubBranchCommitEvent> events) {
        this.events = events;
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

    private GitHubBranchCause toCause(GHBranch remoteBranch, GitHubBranchRepository localRepo, boolean skip, String message,
            Object... args) {
        return new GitHubBranchCause(remoteBranch, localRepo, String.format(message, args), skip);
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchEventDescriptor {

        private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchTrigger.class);

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

        public List<GitHubBranchCommitEventDescriptor> getEventDescriptors() {
            LOG.error("branch commit events");

            return GitHubBranchCommitEventDescriptor.all();
        }
    }
}
