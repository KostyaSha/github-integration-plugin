package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNonMergeableEvent;

def f = namespace(lib.FormTagLib);

f.entry(field: "skip", title: "Skip building unmergable pull requests?") {
    f.checkbox()
}
