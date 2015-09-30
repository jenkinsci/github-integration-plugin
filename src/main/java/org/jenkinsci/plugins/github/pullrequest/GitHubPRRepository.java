package org.jenkinsci.plugins.github.pullrequest;

import com.cloudbees.jenkins.GitHubWebHook;
import hudson.BulkChange;
import hudson.Functions;
import hudson.XmlFile;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub Repository local state = last trigger run() state.
 * Store only necessary variables.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubPRRepository implements Action, Saveable {
    /**
     * Store constantly changing information in project directory with .runtime.xml tail
     */
    public static final String FILE = GitHubPRRepository.class.getName() + ".runtime.xml";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRRepository.class);
    private transient XmlFile configFile; // for save()
    private transient AbstractProject<?, ?> project;  // for UI

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
    public Map<Integer, List<AbstractBuild<?, ?>>> getAllPrBuilds() {

        Map<Integer, List<AbstractBuild<?, ?>>> map = new HashMap<>();
        final RunList<? extends AbstractBuild<?, ?>> builds = project.getBuilds();
        LOGGER.debug("Got builds for project {}", project.getFullName());

        for (AbstractBuild build : builds) {
            GitHubPRCause cause = (GitHubPRCause) build.getCause(GitHubPRCause.class);
            if (cause != null) {
                int number = cause.getNumber();
                List<AbstractBuild<?, ?>> buildsByNumber = map.get(number);
                if (buildsByNumber == null) {
                    buildsByNumber = new ArrayList<>();
                    map.put(number, buildsByNumber);
                }
                buildsByNumber.add(build);
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

    @RequirePOST
    public FormValidation doRebuildFailed() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                Map<Integer, List<AbstractBuild<?, ?>>> builds = getAllPrBuilds();
                for (List<AbstractBuild<?, ?>> buildList : builds.values()) {
                    if (!buildList.isEmpty() && Result.FAILURE.equals(buildList.get(0).getResult())) {
                        AbstractBuild<?, ?> lastBuild = buildList.get(0);
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

            Map<Integer, List<AbstractBuild<?, ?>>> builds = getAllPrBuilds();
            List<AbstractBuild<?, ?>> prBuilds = builds.get(prId);
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

    private static boolean rebuild(AbstractBuild<?, ?> build) {
        return build.getProject().scheduleBuild(0,
                new Cause.UserIdCause(),
                build.getAction(ParametersAction.class),
                build.getAction(CauseAction.class)
        );
    }

    public void setProject(AbstractProject<?, ?> project) {
        this.project = project;
    }

    public void setConfigFile(XmlFile configFile) {
        this.configFile = configFile;
    }
}
