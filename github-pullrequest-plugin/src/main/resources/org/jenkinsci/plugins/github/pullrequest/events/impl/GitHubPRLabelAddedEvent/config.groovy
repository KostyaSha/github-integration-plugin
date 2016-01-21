package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelAddedEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry() {
    f.property(field: "label")
}
