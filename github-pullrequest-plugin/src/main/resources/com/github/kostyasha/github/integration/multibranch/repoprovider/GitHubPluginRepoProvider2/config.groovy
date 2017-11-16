package com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubPluginRepoProvider2

import com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubPluginRepoProvider2
import lib.FormTagLib

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubPluginRepoProvider2();
}

f.entry(title: "Cache connection?", field: "cacheConnection") {
    f.checkbox(default: false)
}

f.entry(title: "Manage hooks", field: "manageHooks") {
    f.checkbox()
}

f.entry(title: "Repository requested permission", field: "repoPermission") {
    f.enum() {
        text(my.name())
    }
}
