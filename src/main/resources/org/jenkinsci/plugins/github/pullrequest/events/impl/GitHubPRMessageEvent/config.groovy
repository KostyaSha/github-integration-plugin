package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRMessageEvent;

def f = namespace(lib.FormTagLib);

f.entry(field: "skip", title: "Skip PR?") {
    f.checkbox()
}

f.entry(title:_("Comment"), field:"comment") {
    f.textbox()
}