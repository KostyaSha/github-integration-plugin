package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRSkipLabelEvent;

def f = namespace(lib.FormTagLib);

f.entry() {
    f.property(field:"skipLabel")
}
