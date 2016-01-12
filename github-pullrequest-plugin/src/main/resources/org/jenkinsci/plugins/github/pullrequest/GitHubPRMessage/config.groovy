package org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage

import lib.FormTagLib

def f = namespace(FormTagLib);

f.entry(title: _('Content'), field: 'content') {
    f.expandableTextbox()
}
