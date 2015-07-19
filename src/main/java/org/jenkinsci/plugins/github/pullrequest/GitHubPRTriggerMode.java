package org.jenkinsci.plugins.github.pullrequest;

/**
 * Trigger modes
 *
 * @author Kanstantsin Shautsou
 */
public enum GitHubPRTriggerMode {
    CRON ("Cron with persistence"),
    HEAVY_HOOKS ("Experimental: Hooks with persistence"),
    HEAVY_HOOKS_CRON ("NOT SUPPORTED: Hooks with persistence and periodic cron check"),
    LIGHT_HOOKS("NOT SUPPORTED: Hooks without persistence");

    private final String description;

    GitHubPRTriggerMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
