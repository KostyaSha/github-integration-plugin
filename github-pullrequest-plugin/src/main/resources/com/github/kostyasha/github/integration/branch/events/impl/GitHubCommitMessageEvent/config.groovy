package com.github.kostyasha.github.integration.branch.events.impl.GitHubCommitMessageEvent

import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

f.entry(field: "skip", title: "Skip with matching pattern?") {
    f.checkbox()
}

f.entry(title: "Message Patterns") {
    f.textarea(field: "matchPatternsStr")
}
