package org.jenkinsci.plugins.github.pullrequest.trigger;

import jenkins.model.CauseOfInterruption;

/**
 * @author Kanstantsin Shautsou
 */
public class NewPRInterruptCause extends CauseOfInterruption {
    @Override
    public String getShortDescription() {
        return "Newer PR will be scheduled.";
    }
}
