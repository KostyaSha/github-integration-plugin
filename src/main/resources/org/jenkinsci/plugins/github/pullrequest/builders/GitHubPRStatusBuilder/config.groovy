package org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder

def f = namespace(lib.FormTagLib);

f.entry(title:_("Build status message")) {
    f.property(field: "statusMessage")
}
