package org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder

import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder

def f = namespace(lib.FormTagLib);

// Stapler doesn't produce default values, so recreate object
if (instance == null) {
    instance = new GitHubPRStatusBuilder()
}

f.entry(title:_("Build status message")) {
    f.property(field: "statusMessage")
}
