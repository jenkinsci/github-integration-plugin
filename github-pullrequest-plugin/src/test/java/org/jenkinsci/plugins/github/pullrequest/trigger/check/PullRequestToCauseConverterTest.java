package org.jenkinsci.plugins.github.pullrequest.trigger.check;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelNotExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;
import org.jenkinsci.plugins.github.pullrequest.util.TaskListenerWrapperRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Predicates.notNull;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.pullrequest.trigger.check.PullRequestToCauseConverter.toGitHubPRCause;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class PullRequestToCauseConverterTest {

    @Mock
    private GitHubPRRepository local;

    @Mock
    private GitHubPRTrigger trigger;

    @Mock
    private GHPullRequest remotePR;

    @Mock
    private GitHubPRPullRequest localPR;

    @Mock
    private GitHubPREvent event;

    @Mock
    private GHRepository remoteRepo;

    @Rule
    public TaskListenerWrapperRule tlRule = new TaskListenerWrapperRule();

    @Mock
    private GHCommitPointer commit;

    @Mock
    private GHUser user;

    @Before
    public void setUp() throws Exception {
        when(remotePR.getUser()).thenReturn(user);
        when(remotePR.getHead()).thenReturn(commit);
        when(remotePR.getBase()).thenReturn(commit);
        when(remotePR.getRepository()).thenReturn(remoteRepo);
        when(remoteRepo.getIssue(Matchers.any(Integer.class))).thenReturn(new GHIssue());
    }

    @Test
    public void shouldCallEventCheck() throws Exception {
        when(local.getPulls()).thenReturn(ImmutableMap.of(1, localPR));
        when(remotePR.getNumber()).thenReturn(1);

        toGitHubPRCause(local, tlRule.getListener(), trigger).toCause(remotePR).apply(event);
        verify(event).check(eq(trigger), eq(remotePR), eq(localPR), any(TaskListener.class));
    }

    @Test
    public void shouldReturnCauseOnSuccessfulOpenEventCheck() throws Exception {
        when(local.getPulls()).thenReturn(new HashMap<Integer, GitHubPRPullRequest>());
        when(remotePR.getNumber()).thenReturn(1);

        GitHubPRCause cause = toGitHubPRCause(local, tlRule.getListener(), trigger)
                .toCause(remotePR)
                .apply(new GitHubPROpenEvent());

        assertThat("open cause", cause, notNullValue(GitHubPRCause.class));
        assertThat("pr num in cause", cause.getNumber(), is(1));
    }

    @Test
    public void shouldReturnCauseOnWhen1OpenedWeGetSecondOneAndHaveEvents() throws Exception {
        when(local.getPulls()).thenReturn(ImmutableMap.of(1, localPR));
        when(remotePR.getNumber()).thenReturn(2);
        when(trigger.getEvents()).thenReturn(asList(
                new GitHubPROpenEvent(),
                new GitHubPRCommitEvent()
        ));

        GitHubPRCause cause = toGitHubPRCause(local, tlRule.getListener(), trigger)
                .apply(remotePR);

        assertThat("open cause", cause, notNullValue(GitHubPRCause.class));
        assertThat("reason in cause", cause.getReason(), containsString("open"));
        assertThat("pr num in cause", cause.getNumber(), is(2));
    }

    /**
     * Test trigger configuration of:
     *
     *     1.) Skip PR if label is not present (when label is not present)
     *     2.) Cause PR if commit changed (when commit has changed)
     *
     * Expected result is that the PR should be skipped. No causes should be
     * identified.
     */
    @Test
    public void shouldSkipEvenThoughNewCommit() throws Exception {
        GHCommitPointer commitPtr = mock(GHCommitPointer.class);
        when(local.getPulls()).thenReturn(ImmutableMap.of(3, localPR));
        when(localPR.getHeadSha()).thenReturn("this is not the sha you are looking for");
        when(remotePR.getNumber()).thenReturn(3);
        when(remotePR.getState()).thenReturn(GHIssueState.OPEN);
        when(remotePR.getHead()).thenReturn(commitPtr);
        when(trigger.getEvents()).thenReturn(asList(
                new GitHubPRLabelNotExistsEvent(new GitHubPRLabel("notfound"), true),
                new GitHubPRCommitEvent()));

        List<GitHubPRCause> causes = from(ImmutableSet.of(remotePR))
                .transform(
                        toGitHubPRCause(local, tlRule.getListener(), trigger)
                )
                .filter(notNull())
                .toList();

        assertThat("no cause", causes, hasSize(0));
    }
}
