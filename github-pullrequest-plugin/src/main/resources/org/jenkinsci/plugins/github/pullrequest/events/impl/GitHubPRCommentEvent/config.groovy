package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommentEvent;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Comment"), field:"comment") {
    f.textbox()
}