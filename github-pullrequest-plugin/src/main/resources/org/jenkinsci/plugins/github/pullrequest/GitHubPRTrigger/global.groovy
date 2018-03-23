package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

import lib.FormTagLib

def f = namespace(FormTagLib);

f.section(title: _(descriptor.displayName)) {
    f.entry(title: _("Published Jenkins URL"), field: "publishedURL") {
        f.textbox()
    }
    f.entry(title: _("GitHub Users and emails"), field: "usersAndEmails") {
        f.expandableTextbox(true)
    }
}
