package org.jenkinsci.plugins.github_integration.hamcrest;

import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHCommitStatus;

import static org.hamcrest.core.Is.is;

/**
 * @author Kanstantsin Shautsou
 */
public class CommitStatusMatcher extends FeatureMatcher<GHCommitStatus, Boolean> {
    private String context;
    private GHCommitState state;
    private String description;

    public CommitStatusMatcher(Matcher<? super Boolean> subMatcher,
                               String context,
                               GHCommitState state,
                               String description) {
        super(subMatcher, "", "");
        this.context = context;
        this.state = state;
        this.description = description;
    }

    @Override
    protected Boolean featureValueOf(GHCommitStatus commitStatus) {
        return commitStatus.getState().equals(state) &&
                commitStatus.getContext().equals(context) &&
                commitStatus.getDescription().equals(description);
    }

    @Factory
    public static CommitStatusMatcher commitStatus(String context,
                                                   GHCommitState state,
                                                   String description) {
        return new CommitStatusMatcher(is(true), context, state, description);
    }
}
