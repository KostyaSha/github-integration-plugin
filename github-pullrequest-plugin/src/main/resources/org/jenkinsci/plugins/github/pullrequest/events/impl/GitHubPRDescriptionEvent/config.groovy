package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRDescriptionEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Skip build phrase"), field: "skipMsg") {
    f.textbox()
}
