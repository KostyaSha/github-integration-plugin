package org.jenkinsci.plugins.github.pullrequest;

/**
 * Trigger modes
 *
 * @author Kanstantsin Shautsou
 */
public enum GitHubPRTriggerMode {
    CRON ("Cron"),
    HEAVY_HOOKS ("Hooks"),
    HEAVY_HOOKS_CRON ("NOT SUPPORTED: Hooks plus Cron"),
    LIGHT_HOOKS("NOT SUPPORTED: Light Hooks");

    private final String description;

    GitHubPRTriggerMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
