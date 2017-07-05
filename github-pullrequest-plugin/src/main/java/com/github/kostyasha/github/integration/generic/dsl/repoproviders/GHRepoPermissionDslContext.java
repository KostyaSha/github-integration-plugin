package com.github.kostyasha.github.integration.generic.dsl.repoproviders;

import com.github.kostyasha.github.integration.generic.repoprovider.GHPermission;
import javaposse.jobdsl.dsl.Context;

/**
 * @author Kanstantsin Shautsou
 */
public class GHRepoPermissionDslContext implements Context {
    private GHPermission permission = GHPermission.ADMIN;

    public void admin() {
        permission = GHPermission.ADMIN;
    }

    public void pull() {
        permission = GHPermission.PULL;
    }

    public void push() {
        permission = GHPermission.PUSH;
    }

    public GHPermission permission() {
        return permission;
    }
}
