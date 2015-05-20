package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRDescriptionEvent;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Skip build phrase"), field:"skipMsg") {
    f.textbox()
}
