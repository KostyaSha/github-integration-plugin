package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommentEvent

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Comment Pattern"), field: "comment") {
    f.textbox()
}
