//package org.jenkinsci.plugins.github.pullrequest;
//
//import hudson.Extension;
//import hudson.model.AbstractBuild;
//import hudson.model.ParametersAction;
//import hudson.model.Run;
//import hudson.model.TaskListener;
//import hudson.plugins.git.GitException;
//import hudson.plugins.git.Revision;
//import hudson.plugins.git.util.*;
//import hudson.util.RunList;
//import org.jenkinsci.plugins.gitclient.GitClient;
//import org.kohsuke.stapler.DataBoundConstructor;
//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.List;
//
///**
// * @author Kanstantsin Shautsou
// */
//public class GitHubPRBuildChooser extends DefaultBuildChooser {
//
//    @DataBoundConstructor
//    public GitHubPRBuildChooser() {
//    }
//
//    @Override
//    public Build prevBuildForChangelog(String branch, BuildData data, GitClient git, BuildChooserContext context)
//          throws IOException, InterruptedException {
//        BuildData previousBuildData = getPreviousBuildDataFor(context.getBuild());
//        if (previousBuildData != null){
//            return previousBuildData.lastBuild;
//        }
//        return null;
//    }
//
//    @Override
//    public BuildData getPreviousBuildDataFor(AbstractBuild<?,?> build) {
//        GitHubPRCause currCause = build.getCause(GitHubPRCause.class);
//
//        RunList<? extends AbstractBuild<?, ?>> builds = build.getProject().getBuilds();
//        for (Run<?, ?> r : builds) {
//            GitHubPRCause cause = r.getCause(GitHubPRCause.class);
//            if (cause == null) {
//                continue;
//            }
//            if (cause.getNumber() != currCause.getNumber()){
//                continue;
//            }
//            return r.getAction(BuildData.class);
//        }
//
//        return null;
//    }
//
//    @Extension
//    public static final class DescriptorImpl extends BuildChooserDescriptor {
//        @Override
//        public String getDisplayName() {
//            return "GitHub Pull Request";
//        }
//    }
//
//}
