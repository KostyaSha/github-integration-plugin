package com.github.kostyasha.github.integration.branch.GitHubBranchCause

import lib.FormTagLib

def f = namespace(FormTagLib);

if (my.htmlUrl != null) {
    text("GitHub branch ")
    a(href: "${my.htmlUrl}/tree/${my.branchName}") {
        text("${my.branchName}")
    }
    text(": ${my.reason}")
} else {
    text("Branch deleted")
}
