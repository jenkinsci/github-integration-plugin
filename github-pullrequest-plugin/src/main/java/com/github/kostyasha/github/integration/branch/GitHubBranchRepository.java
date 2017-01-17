package com.github.kostyasha.github.integration.branch;

import com.cloudbees.jenkins.GitHubWebHook;
import com.github.kostyasha.github.integration.generic.GitHubRepository;
import hudson.Functions;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.FormValidation;
import hudson.util.RunList;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchCauseFromRun;
import static com.github.kostyasha.github.integration.branch.utils.JobHelper.ghBranchTriggerFromJob;
import static java.util.Objects.isNull;
import static org.jenkinsci.plugins.github.pullrequest.utils.JobHelper.rebuild;

/**
 * Store local state of remote branches.
 *
 * @author Kanstantsin Shautsou
 */
public class GitHubBranchRepository extends GitHubRepository<GitHubBranchRepository> {
    /**
     * Store constantly changing information in job directory with .runtime.xml tail
     */
    public static final String FILE = GitHubBranchRepository.class.getName() + ".runtime.xml";
    private static final Logger LOG = LoggerFactory.getLogger(GitHubBranchRepository.class);

    private Map<String, GitHubBranch> branches = new HashMap<>();

    /**
     * Object that represent GitHub repository to work with
     *
     * @param remoteRepository remote repository full name.
     */
    public GitHubBranchRepository(GHRepository remoteRepository) {
        super(remoteRepository);
    }

    @Nonnull
    public Map<String, GitHubBranch> getBranches() {
        if (isNull(branches)) {
            branches = new HashMap<>();
        }
        return branches;
    }

    @Override
    public String getIconFileName() {
        return Functions.getResourcePath() + "/plugin/github-pullrequest/git-branch.svg";
    }

    @Override
    public String getDisplayName() {
        return "GitHub Branches";
    }

    @Override
    public String getUrlName() {
        return "github-branch";
    }


    /**
     * Searches for all builds performed in the runs of current job.
     *
     * @return map with keys - string branch names and values - lists of related builds.
     */
    public Map<String, List<Run<?, ?>>> getAllBranchBuilds() {

        Map<String, List<Run<?, ?>>> map = new HashMap<>();
        final RunList<?> runs = job.getBuilds();
        LOG.debug("Got builds for job {}", job.getFullName());

        for (Run<?, ?> run : runs) {
            GitHubBranchCause cause = ghBranchCauseFromRun(run);
            if (cause != null) {
                String branchName = cause.getBranchName();
                List<Run<?, ?>> buildsByBranchName = map.get(branchName);
                if (isNull(buildsByBranchName)) {
                    buildsByBranchName = new ArrayList<>();
                    map.put(branchName, buildsByBranchName);
                }
                buildsByBranchName.add(run);
            }
        }

        return map;
    }

    @Override
    @RequirePOST
    public FormValidation doClearRepo() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.DELETE)) {
                branches = new HashMap<>();
                save();
                result = FormValidation.ok("Branches deleted");
            } else {
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't delete repository file '{}'.",
                    configFile.getFile().getAbsolutePath(), e);
            result = FormValidation.error(e, "Can't delete: " + e.getMessage());
        }
        return result;
    }

    @Override
    @RequirePOST
    public FormValidation doRunTrigger() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                GitHubBranchTrigger trigger = ghBranchTriggerFromJob(job);
                if (trigger != null) {
                    trigger.run();
                    result = FormValidation.ok("GitHub Branch trigger run");
                    LOG.debug("GitHub Branch trigger run for {}", job);
                } else {
                    LOG.error("GitHub Branch trigger not available for {}", job);
                    result = FormValidation.error("GitHub Branch trigger not available");
                }
            } else {
                LOG.warn("No permissions to run GitHub Branch trigger");
                result = FormValidation.error("Forbidden");
            }
        } catch (Exception e) {
            LOG.error("Can't run trigger", e.getMessage());
            result = FormValidation.error(e, "Can't run trigger: %s", e.getMessage());
        }
        return result;
    }

    @Override
    @RequirePOST
    public FormValidation doRebuildFailed() throws IOException {
        FormValidation result;
        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (instance.hasPermission(Item.BUILD)) {
                Map<String, List<Run<?, ?>>> builds = getAllBranchBuilds();
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
            LOG.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error(e, "Can't start rebuild: %s", e.getMessage());
        }
        return result;
    }

    @Override
    @RequirePOST
    public FormValidation doRebuild(StaplerRequest req) throws IOException {
        FormValidation result;

        try {
            Jenkins instance = GitHubWebHook.getJenkinsInstance();
            if (!instance.hasPermission(Item.BUILD)) {
                return FormValidation.error("Forbidden");
            }

            final String param = "branchName";
            String branchName = "";
            if (req.hasParameter(param)) {
                branchName = req.getParameter(param);
            }

            Map<String, List<Run<?, ?>>> allBuilds = getAllBranchBuilds();
            List<Run<?, ?>> branchBuilds = allBuilds.get(branchName);
            if (branchBuilds != null && !allBuilds.isEmpty()) {
                if (rebuild(branchBuilds.get(0))) {
                    result = FormValidation.ok("Rebuild scheduled");
                } else {
                    result = FormValidation.warning("Rebuild not scheduled");
                }
            } else {
                result = FormValidation.warning("Build not found");
            }
        } catch (Exception e) {
            LOG.error("Can't start rebuild", e.getMessage());
            result = FormValidation.error(e, "Can't start rebuild: " + e.getMessage());
        }
        return result;
    }

}
