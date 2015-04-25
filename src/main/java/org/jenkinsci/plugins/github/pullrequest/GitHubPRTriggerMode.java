package org.jenkinsci.plugins.github.pullrequest;

/**
 * Trigger mode
 *
 * @author Kanstantsin Shautsou
 */
enum GitHubPRTriggerMode {
    CRON ("Cron with persistence"),
    LIGHT_HOOKS("Hooks without persistence"),
    HEAVY_HOOKS ("Hooks with persistence"),
    HEAVY_HOOKS_CRON ("Hooks with persistence and periodic cron check");


    private final String description;

    GitHubPRTriggerMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
