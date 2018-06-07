package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

import lib.FormTagLib

def f = namespace(FormTagLib);

f.section(title: _(descriptor.displayName)) {
    f.entry(title: _("Published Jenkins URL"), field: "publishedURL") {
        f.textbox()
    }

    f.entry(title: "Actualise local repo on factory creation", field: "actualiseOnFactory") {
        f.checkbox(default: false)
    }
}
