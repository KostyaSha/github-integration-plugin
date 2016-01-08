package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelPatternExistsEvent;

def f = namespace(lib.FormTagLib);

f.entry(field: "skip", title: "Skip pull requests with label(s) matching pattern?") {
    f.checkbox()
}

f.entry() {
    f.property(field:"label")
}
