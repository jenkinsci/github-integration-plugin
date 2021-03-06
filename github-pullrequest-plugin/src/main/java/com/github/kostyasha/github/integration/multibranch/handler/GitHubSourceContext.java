package com.github.kostyasha.github.integration.multibranch.handler;

import com.github.kostyasha.github.integration.generic.GitHubCause;
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource;
import com.github.kostyasha.github.integration.multibranch.action.GitHubRepo;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;
import hudson.model.Job;
import hudson.model.TaskListener;
import jenkins.branch.BranchProjectFactory;
import jenkins.branch.MultiBranchProject;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadEvent;
import jenkins.scm.api.SCMHeadObserver;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSourceCriteria;
import jenkins.scm.api.SCMSourceOwner;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;

import static java.util.Objects.nonNull;

public class GitHubSourceContext {

    private final GitHubSCMSource source;
    private final SCMHeadObserver observer;
    private final SCMSourceCriteria criteria;
    private final SCMHeadEvent<?> scmHeadEvent;
    private final GitHubRepo localRepo;
    private final GHRepository remoteRepo;
    private final TaskListener listener;

    public GitHubSourceContext(@NonNull GitHubSCMSource source,
                               @NonNull SCMHeadObserver observer,
                               @NonNull SCMSourceCriteria criteria,
                               @Nullable SCMHeadEvent<?> scmHeadEvent,
                               @NonNull GitHubRepo localRepo,
                               @NonNull GHRepository remoteRepo,
                               @NonNull TaskListener listener) {
        this.source = source;
        this.observer = observer;
        this.criteria = criteria;
        this.scmHeadEvent = scmHeadEvent;
        this.localRepo = localRepo;
        this.remoteRepo = remoteRepo;
        this.listener = listener;
    }

    @NonNull
    public GitHubSCMSource getSource() {
        return source;
    }

    @NonNull
    public SCMHeadObserver getObserver() {
        return observer;
    }

    public SCMHeadEvent<?> getScmHeadEvent() {
        return scmHeadEvent;
    }

    @NonNull
    public GitHubRepo getLocalRepo() {
        return localRepo;
    }

    @NonNull
    public GHRepository getRemoteRepo() {
        return remoteRepo;
    }

    @NonNull
    public TaskListener getListener() {
        return listener;
    }

    public GitHub getGitHub() {
        return source.getRepoProvider().getGitHub(source);
    }

    public boolean checkCriteria(@NonNull GitHubCause<?> cause) throws IOException {
        if (nonNull(criteria)) {
            GitHubSCMRevision revision = cause.createSCMRevision(source.getId());
            listener.getLogger().println("");
            listener.getLogger().println("Checking " + revision.getHead().getPronoun());
            if (!criteria.isHead(source.newProbe(revision.getHead(), revision), listener)) {
                listener.getLogger().println("  Didn't meet criteria");
                return false;
            }
            listener.getLogger().println("  Met criteria");
        }
        return true;
    }

    public void observe(@NonNull GitHubCause<?> cause) {
        try {
            GitHubSCMRevision scmRevision = cause.createSCMRevision(source.getId());
            if (!cause.isSkip()) {
                forceScheduling(scmRevision);
            }
            getObserver().observe(scmRevision.getHead(), scmRevision);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace(listener.getLogger());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void forceScheduling(GitHubSCMRevision scmRevision) throws IOException {
        SCMSourceOwner owner = source.getOwner();
        if (owner instanceof MultiBranchProject) {
            MultiBranchProject mb = (MultiBranchProject) owner;
            BranchProjectFactory pf = mb.getProjectFactory();

            SCMHead scmHead = scmRevision.getHead();
            Job j = mb.getItemByBranchName(scmHead.getName());
            if (j != null) {
                SCMRevision rev = pf.getRevision(j);
                // set current rev to dummy to force scheduling
                if (rev != null && rev.equals(scmRevision)) {
                    pf.setRevisionHash(j, new DummyRevision(scmHead));
                }
            }
        }
    }

    /**
     * Special revision to unconditionally force next build
     */
    public static class DummyRevision extends SCMRevision {
        private static final long serialVersionUID = 1L;

        public DummyRevision(SCMHead head) {
            super(head);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }
}
