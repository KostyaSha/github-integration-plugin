package org.jenkinsci.plugins.github.pullrequest.GitHubPRCause

import lib.FormTagLib

def f = namespace(FormTagLib);

if (my.htmlUrl != null) {
    text("GitHub PR ")
    a(href: "${my.htmlUrl}") {
        text("#${my.number}")
    }
    text(": ${my.reason}")
} else {
    text("GitHub PR deleted")
}
