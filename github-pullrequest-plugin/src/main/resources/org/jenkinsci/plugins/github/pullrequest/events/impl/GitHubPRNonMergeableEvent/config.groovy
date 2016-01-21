package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNonMergeableEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(field: "skip", title: "Skip building unmergable pull requests?") {
    f.checkbox()
}
