package org.jenkinsci.plugins.github.pullrequest.GitHubPRMessage

def f = namespace(lib.FormTagLib);

f.entry(title: _("Message content"), help: descriptor.getHelpFile('content')) {
    f.expandableTextbox(field: "content")
}
