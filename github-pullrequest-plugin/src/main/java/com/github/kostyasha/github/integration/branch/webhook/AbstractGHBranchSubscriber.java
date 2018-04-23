package com.github.kostyasha.github.integration.branch.webhook;

import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;

import java.io.IOException;
import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;
import static net.sf.json.JSONObject.fromObject;

/**
 * @author Kanstantsin Shautsou
 */
public abstract class AbstractGHBranchSubscriber extends GHEventsSubscriber {
    public static final Set<GHEvent> EVENTS = immutableEnumSet(GHEvent.PUSH, GHEvent.CREATE, GHEvent.DELETE);

    @Override
    protected Set<GHEvent> events() {
        return EVENTS;
    }

    protected BranchInfo extractRefInfo(GHEvent event, String payload) throws IOException {
        return extractRefInfo(event, payload, false);
    }

    protected BranchInfo extractRefInfo(GHEvent event, String payload, boolean tagsAware) throws IOException {
        JSONObject json = fromObject(payload);
        if (EVENTS.contains(event)) {
            return fromJson(json, tagsAware);
        } else {
            throw new IllegalStateException(format("Did you add event %s in events() method?", event));
        }
    }

    protected BranchInfo fromJson(JSONObject json) {
        return fromJson(json, false);
    }

    protected BranchInfo fromJson(JSONObject json, boolean tagsAware) {
        JSONObject jsonRepository = json.getJSONObject("repository");
        final String repo = jsonRepository.getString("full_name");

        String branchName = json.getString("ref");
        String refType = json.optString("ref_type", null);
        String fullRef = branchName;

        boolean tag = false;

        if (branchName.startsWith("refs/heads/")) {
            branchName = branchName.replace("refs/heads/", "");
        }

        if ("tag".equals(refType)) {
            tag = true;
        } else if (branchName.startsWith("refs/tags/")) {
            // backwards compatibility
            if (tagsAware) {
                branchName = branchName.replace("refs/tags/", "");
            }
            tag = true;
        }

        return new BranchInfo(repo, branchName, fullRef, tag);
    }
}
