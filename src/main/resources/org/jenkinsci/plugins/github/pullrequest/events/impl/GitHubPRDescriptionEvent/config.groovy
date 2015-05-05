package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRDescriptionEvent;

def f = namespace(lib.FormTagLib);

f.entry(field: "skip", title: "Skip PR?") {
    f.checkbox()
}

f.entry(title:_("Skip build phrase"), field:"skipMsg") {
    f.textbox()
}
