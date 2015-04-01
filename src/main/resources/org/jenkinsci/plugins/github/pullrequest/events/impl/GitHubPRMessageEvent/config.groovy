package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRMessageEvent;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Trigger phrase"), field:"runMsg") {
    f.textbox()
}