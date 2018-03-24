package com.github.kostyasha.github.integration.branch.GitHubBranchBadgeAction

import lib.FormTagLib
import lib.LayoutTagLib
import com.github.kostyasha.github.integration.branch.GitHubBranch

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

a(href: my.htmlUrl) {
    img(src: "${rootURL}${GitHubBranch.iconFileName}",
            title: my.title,
            width: "16",
            height: "16"
    )
    text("${my.getBranchName()}")
}
