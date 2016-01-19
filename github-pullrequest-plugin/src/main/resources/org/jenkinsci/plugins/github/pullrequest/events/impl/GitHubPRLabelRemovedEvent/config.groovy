package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelRemovedEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry() {
    f.property(field: "label")
}
