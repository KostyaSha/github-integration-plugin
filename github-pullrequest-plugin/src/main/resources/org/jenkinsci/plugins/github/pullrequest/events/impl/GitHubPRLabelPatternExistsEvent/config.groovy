package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelPatternExistsEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(field: "skip", title: "Skip pull requests with label(s) matching pattern?") {
    f.checkbox()
}

f.entry() {
    f.property(field: "label")
}
