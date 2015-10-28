package org.jenkinsci.plugins.github.pullrequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a comment for GitHub that can contain token macros.
 *
 * @author Kanstantsin Shautsou
 * @author Alina Karpovich
 */
public class GitHubPRMessage extends AbstractDescribableImpl<GitHubPRMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPRMessage.class);

    private String content;

    @DataBoundConstructor
    public GitHubPRMessage(String content) {
        this.content = content;
    }

    /**
     * Expand all what we can from given run
     */
    @Restricted(NoExternalUse.class)
    @CheckForNull
    public String expandAll(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        return expandAll(getContent(), run, listener);
    }

    /**
     * Expand all what we can from given run
     */
    @Restricted(NoExternalUse.class)
    @CheckForNull
    public static String expandAll(String body, Run<?, ?> run, TaskListener listener)
            throws IOException, InterruptedException {
        if (body == null || body.length() == 0) {
            return body; // Do nothing for an empty String
        }

        // Expand environment variables
        body = run.getEnvironment(listener).expand(body);

        // Expand build variables + token macro if they available
        if (run instanceof AbstractBuild<?, ?>) {
            final AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) run;
            body = Util.replaceMacro(body, build.getBuildVariableResolver());

            try {
                Jenkins jenkins = Jenkins.getActiveInstance();
                ClassLoader uberClassLoader = jenkins.pluginManager.uberClassLoader;
                List macros = null;
                if (jenkins.getPlugin("token-macro") != null) {
                    // get private macroses like groovy template ${SCRIPT} if available
                    if (jenkins.getPlugin("email-ext") != null) {
                        Class<?> contentBuilderClazz = uberClassLoader.loadClass("hudson.plugins.emailext.plugins.ContentBuilder");
                        Method getPrivateMacrosMethod = contentBuilderClazz.getDeclaredMethod("getPrivateMacros");
                        macros = new ArrayList((Collection) getPrivateMacrosMethod.invoke(null));
                    }

                    // call TokenMacro.expand(build, listener, content, false, macros)
                    Class<?> tokenMacroClazz = uberClassLoader.loadClass("org.jenkinsci.plugins.tokenmacro.TokenMacro");
                    Method tokenMacroExpand = tokenMacroClazz.getDeclaredMethod("expand", AbstractBuild.class,
                            TaskListener.class, String.class, boolean.class, List.class);

                    body = (String) tokenMacroExpand.invoke(null, build, listener, body, false, macros);
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Can't find class", e);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                LOGGER.error("Can't evaluate macro", e);
            }
        }

        return body;
    }

    public String getContent() {
        return content;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubPRMessage> {
        @Override
        public String getDisplayName() {
            return "Expandable comment field";
        }
    }
}
