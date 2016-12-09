package com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider

import com.github.kostyasha.github.integration.generic.repoprovider.GitHubPluginRepoProvider
import lib.FormTagLib

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubPluginRepoProvider();
}

f.entry(title: "Cache connection?", field: "cacheConnection") {
    f.checkbox(default: false)
}