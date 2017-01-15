package com.github.kostyasha.github.integration.branch.events.impl;

import com.github.kostyasha.github.integration.branch.GitHubBranch;
import com.github.kostyasha.github.integration.branch.GitHubBranchCause;
import com.github.kostyasha.github.integration.branch.GitHubBranchRepository;
import com.github.kostyasha.github.integration.branch.GitHubBranchTrigger;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEvent;
import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor;

import hudson.Extension;
import hudson.model.TaskListener;

import org.kohsuke.github.GHBranch;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHubBranchRestrictionFilter extends GitHubBranchEvent {

    private static final String DISPLAY_NAME = "Branch Restrictions";

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubBranchRestrictionFilter.class);

    private boolean exclude;

    private boolean matchAsPattern;
    private Set<String> matchCriteria;

    @DataBoundConstructor
    public GitHubBranchRestrictionFilter() {
    }

    public String getMatchCriteriaStr() {
        return String.join(LINE_SEPARATOR, matchCriteria);
    }

    @DataBoundSetter
    public void setMatchCriteriaStr(String matchCriteria) {
        this.matchCriteria = Stream.of(matchCriteria
                .split(LINE_SEPARATOR))
                .collect(Collectors.toSet());
    }

    public boolean isExclude() {
        return exclude;
    }

    @DataBoundSetter
    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public boolean isMatchAsPattern() {
        return matchAsPattern;
    }

    @DataBoundSetter
    public void setMatchAsPattern(boolean matchAsPattern) {
        this.matchAsPattern = matchAsPattern;
    }

    // visible for testing
    public Set<String> getMatchCriteria() {
        return matchCriteria;
    }

    @Override
    public GitHubBranchCause check(GitHubBranchTrigger gitHubBranchTrigger, GHBranch remoteBranch, GitHubBranch localBranch,
                                   GitHubBranchRepository localRepo, TaskListener listener) {
        String name = remoteBranch.getName();
        if (matchCriteria.isEmpty() || branchIsAllowed(name)) {
            if (matchCriteria.isEmpty()) {
                LOGGER.warn("Branch restriction filter added but no match criteria set, all branches allowed");
            }
            // filter allows checking other events
            return null;
        }

        return toCause(remoteBranch, localRepo, true, "Branch [%s] filtered by branch name restriction filter", name);
    }

    private boolean branchIsAllowed(String name) {
        for (String pattern : matchCriteria) {
            LOGGER.trace("Checking branch [{}] against pattern [{}] - exclude [{}]", name, pattern, exclude);
            if (matches(name, pattern)) {
                if (exclude) {
                    LOGGER.debug("Branch [{}] matches pattern [{}], will be excluded", name, pattern);
                    return false;
                }
                LOGGER.debug("Branch [%s] matched pattern [{}] and is marked for inclusion", name, pattern);
                return true;
            }
        }
        LOGGER.trace("Branch [{}] matched no patterns, included [{}]", name, exclude);
        return exclude;
    }

    private boolean matches(String name, String pattern) {
        if (!matchAsPattern) {
            LOGGER.debug("Checking branch [{}] against exact match [{}]", name, pattern);
            return name.equals(pattern);
        }

        try {
            LOGGER.debug("Checking branch [{}] against pattern [{}].", name, pattern);
            return Pattern.compile(pattern).matcher(name).matches();
        } catch (PatternSyntaxException e) {
            LOGGER.error("Invalid pattern [{}] detected checking branch [{}]", pattern, name, e);
            return false;
        }
    }

    private GitHubBranchCause toCause(GHBranch remoteBranch, GitHubBranchRepository localRepo, boolean skip,
                                      String message, Object... args) {
        return new GitHubBranchCause(remoteBranch, localRepo, String.format(message, args), skip);
    }

    @Extension
    public static class Descriptor extends GitHubBranchEventDescriptor {
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }

}
