package com.github.kostyasha.github.integration.tag.events.impl.GitHubTagRestrictionFilter

import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

f.entry(field: "exclude", title: "Exclude tags that match criteria?") {
    f.checkbox()
}

f.entry(field: "matchAsPattern", title: "Match tags against pattern criteria?") {
    f.checkbox()
}

f.entry(title: "Tags Names or Patterns") {
    f.textarea(field: "matchCriteriaStr")
}
