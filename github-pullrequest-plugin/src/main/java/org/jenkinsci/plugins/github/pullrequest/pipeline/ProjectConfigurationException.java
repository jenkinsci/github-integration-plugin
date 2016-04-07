package org.jenkinsci.plugins.github.pullrequest.pipeline;

/**
 * Raised when the project is configured improperly. Certain steps require particular
 * project properties to be enabled or set. If the step cannot find the setting it
 * needs it will raise this error to indicate the missing or incorrect attribute.
 */
public class ProjectConfigurationException extends Exception {

    public ProjectConfigurationException(String message) {
        super(message);
    }
}
