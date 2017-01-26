package com.github.kostyasha.github.integration.branch.events.impl.commitchecks.impl.GitHubBranchCommitMessageCheck

import lib.FormTagLib

def f = namespace(FormTagLib);

f.entry(field: "exclude", title: "Exclude commits that match criteria?") {
    f.checkbox()
}

f.entry(title: _("Message Patterns"), field: "matchCriteria") {
    f.expandableTextbox()
}
