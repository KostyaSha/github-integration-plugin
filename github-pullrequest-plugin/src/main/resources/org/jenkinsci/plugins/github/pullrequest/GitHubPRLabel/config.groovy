package org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Labels"), field: "labels") {
    f.expandableTextbox()
}
