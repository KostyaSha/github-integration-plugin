package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNumber

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(field: "number", title: "PR Number") {
    f.number()
}

f.entry(field: "match", title: "Match number?") {
    f.checkbox()
}

f.entry(field: "skip", title: "Skip when matching?") {
    f.checkbox()
}
