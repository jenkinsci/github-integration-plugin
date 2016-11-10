package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheck;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchCommitCheckDescriptor;
import hudson.Extension;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCompare.Commit;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHubBranchCommitMessageCheck extends GitHubBranchCommitCheck {
    private static final String DISPLAY_NAME = "Commit Message Pattern";

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchCommitMessageCheck.class);

    private boolean exclude;

    private Set<String> matchCriteria;

    @DataBoundConstructor
    public GitHubBranchCommitMessageCheck() {
        this.matchCriteria = Collections.emptySet();
    }

    @Override
    public GitHubBranchCause check(GHBranch remoteBranch, GitHubBranchRepository localRepo, Commit[] commits) {
        String name = remoteBranch.getName();
        if (matchCriteria.isEmpty()) {
            LOGGER.warn("Commit message event added but no match criteria set, all commits allowed");
            return toCause(remoteBranch, localRepo, false, "No match, commits for branch [%s] allowed by message event.", name);
        }

        List<String> messages = Stream.of(commits)
                .map(commit -> commit.getCommit().getMessage())
                .collect(Collectors.toList());

        if (commitsAreAllowed(messages)) {
            return toCause(remoteBranch, localRepo, false, "Commit messages %s for branch [%s] allowed by message event.", messages, name);
        }

        return toCause(remoteBranch, localRepo, true, "Commit messages %s for branch [%s] not allowed by message event.", messages, name);
    }

    public String getMatchCriteria() {
        return String.join(LINE_SEPARATOR, matchCriteria);
    }

    public boolean isExclude() {
        return exclude;
    }

    @DataBoundSetter
    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    @DataBoundSetter
    public void setMatchCriteria(String matchCriteria) {
        this.matchCriteria = Stream.of(matchCriteria
                .split(LINE_SEPARATOR))
                .collect(Collectors.toSet());
    }

    private boolean commitsAreAllowed(List<String> messages) {
        boolean allowed = false;
        for (String message : messages) {
            if (commitIsAllowed(message)) {
                allowed = true;
                break;
            }
        }
        return allowed;
    }

    private boolean commitIsAllowed(String message) {
        for (String pattern : matchCriteria) {
            LOGGER.debug("Checking commit [{}] against pattern [{}] - exclude [{}]", message, pattern, exclude);
            if (matches(message, pattern)) {
                if (exclude) {
                    LOGGER.debug("Commit [{}] matches pattern [{}], will be excluded", message, pattern);
                    return false;
                }
                LOGGER.debug("Commit [{}] matched pattern [{}] and is marked for inclusion", message, pattern);
                return true;
            }
        }

        LOGGER.debug("Commit [{}] matched no patterns, included [{}]", message, exclude);
        return exclude;
    }

    private boolean matches(String message, String pattern) {
        try {
            return Pattern.compile(pattern).matcher(message).matches();
        } catch (PatternSyntaxException e) {
            LOGGER.error("Invalid pattern [{}] detected checking commit [{}]", pattern, message, e);
            return false;
        }
    }

    private GitHubBranchCause toCause(GHBranch remoteBranch, GitHubBranchRepository localRepo, boolean skip, String message,
            Object... args) {
        return new GitHubBranchCause(remoteBranch, localRepo, String.format(message, args), skip);
    }

    @Extension
    public static class DescriptorImpl extends GitHubBranchCommitCheckDescriptor {
        @Override
        public final String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
