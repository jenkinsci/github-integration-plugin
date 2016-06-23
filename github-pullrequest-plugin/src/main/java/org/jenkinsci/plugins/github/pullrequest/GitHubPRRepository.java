package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubWebHook;
import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.BuildBadgeAction;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.listeners.SaveableListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.pullrequest.utils.JobHelper;
import com.github.kostyasha.github.integration.generic.GitHubRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.ghPRCauseFromRun;
import static org.jenkinsci.plugins.github.pullrequest.utils.ObjectsUtil.isNull;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * GitHub Repository local state = last trigger run() state.
 * Store only necessary variables.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRRepository extends GitHubRepository {
    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    public static final String FILE = GitHubPRRepository.class.getName() + ".runtime.xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepository.class);
    private transient XmlFile configFile; // for save()
    private transient Job<?, ?> job;  // for UI

    private final String fullName;
    private final String githubUrl;

    private Map<Integer, GitHubPRPullRequest> pulls;

    /**
     * Object that represent GitHub repository to work with
     *
     * @param fullName repository full name. for case of changed jobs url
     * @param pulls    previous pull request state
     */
    public GitHubPRRepository(String fullName, String githubUrl, Map<Integer, GitHubPRPullRequest> pulls) {
        this.pulls = pulls;
        this.fullName = fullName;
        this.githubUrl = githubUrl;
    }

    public Map<Integer, GitHubPRPullRequest> getPulls() {
        return pulls;
    }

    /**
     * Searches for all builds performed in the runs of current job.
     *
     * @return map with keys - numbers of built PRs and values - lists of related builds.
     */
    public Map<Integer, List<Run<?, ?>>> getAllPrBuilds() {

        Map<Integer, List<Run<?, ?>>> map = new HashMap<>();
        final RunList<?> runs = job.getBuilds();
        LOGGER.debug("Got builds for job {}", job.getFullName());

        for (Run<?, ?> run : runs) {
            GitHubPRCause cause = ghPRCauseFromRun(run);
            if (cause != null) {
                int number = cause.getNumber();
                List<Run<?, ?>> buildsByNumber = map.get(number);
                if (isNull(buildsByNumber)) {
                    buildsByNumber = new ArrayList<>();
                    map.put(number, buildsByNumber);
                }
                buildsByNumber.add(run);
            }
        }

        return map;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-pull-request.svg";
    }

    @Override
    public String getDisplayName() {
        return "GitHub PR";
    }

    @Override
    public String getUrlName() {
        return "github-pullrequest";
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    @Override
    public synchronized void save() throws IOException {
        if (BulkChange.contains(this)) {
            return;
        }

        configFile.write(this);
        SaveableListener.fireOnChange(this, configFile);
    }

    public void saveQuetly() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.error("Can't save repository state, because: '{}'", e.getMessage(), e);
        }
    }

    @RequirePOST
    public FormValidation doClearRepo() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.DELETE)) {
                pulls = new HashMap<>();
                save();
                result = FormValidation.ok("Pulls deleted");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOGGER.error("Can\'t delete repository file '{}', '{}'",
                    configFile.getFile().getAbsolutePath(), e.getMessage());
            result = FormValidation.error("Can't delete: " + e.getMessage());
        }
        return result;
    }


    /**
     * Run trigger from web.
     */
    @RequirePOST
    public FormValidation doRunTrigger() {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                GitHubPRTrigger trigger = JobHelper.ghPRTriggerFromJob(job);
                if (trigger != null) {
                    trigger.run();
                    result = FormValidation.ok("GitHub PR trigger run");
                    LOGGER.debug("GitHub PR trigger run for {}", job);
                } else {
                    LOGGER.error("GitHub PR trigger not available for {}", job);
                    result = FormValidation.error("GitHub PR trigger not available");
                }
            } else {
                LOGGER.warn("No permissions to run GitHub PR trigger");
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOGGER.error("Can't run trigger", e.getMessage());
            result = FormValidation.error("Can't run trigger: %s", e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doRebuildFailed() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                Map<Integer, List<Run<?, ?>>> builds = getAllPrBuilds();
                for (List<Run<?, ?>> buildList : builds.values()) {
                    if (!buildList.isEmpty() && Result.FAILURE.equals(buildList.get(0).getResult())) {
                        Run<?, ?> lastBuild = buildList.get(0);
                        rebuild(lastBuild);
                    }
                }
                result = FormValidation.ok("Rebuild scheduled");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOGGER.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error("Can't start rebuild: %s", e.getMessage());
        }
        return result;
    }

    @RequirePOST
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (!instance.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String prNumberParam = "prNumber";
            int prId = 0;
            if (req.hasParameter(prNumberParam)) {
                prId = Integer.valueOf(req.getParameter(prNumberParam));
            }

            Map<Integer, List<Run<?, ?>>> builds = getAllPrBuilds();
            List<Run<?, ?>> prBuilds = builds.get(prId);
            if (prBuilds != null && !prBuilds.isEmpty()) {
                if (rebuild(prBuilds.get(0))) {
                    result = FormValidation.ok("Rebuild scheduled");
                } else {
                    result = FormValidation.warning("Rebuild not scheduled");
                }
            } else {
                result = FormValidation.warning("Build not found");
            }
        } catch (Exception e) {
            LOGGER.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error("Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

    private static boolean rebuild(Run<?, ?> run) {
        final QueueTaskFuture queueTaskFuture = asParameterizedJobMixIn(run.getParent())
                .scheduleBuild2(
                        0,
                        run.getAction(ParametersAction.class),
                        run.getAction(CauseAction.class),
                        run.getAction(BuildBadgeAction.class)
                );
        return queueTaskFuture != null;
    }

    public void setJob(Job<?, ?> job) {
        this.job = job;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }
}
