package org.jenkinsci.plugins.github.pullrequest;

/**
 * Trigger modes
 *
 * @author Kanstantsin Shautsou
 */
public enum GitHubPRTriggerMode {
    CRON("Cron with Persisted Data"),
    HEAVY_HOOKS("Hooks with Persisted Data"),
    HEAVY_HOOKS_CRON("Hooks plus Cron with Persisted Data"),
    LIGHT_HOOKS("NOT SUPPORTED: Non-persistent Hooks");

    private final String description;

    GitHubPRTriggerMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
