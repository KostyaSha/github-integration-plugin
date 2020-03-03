package com.github.kostyasha.github.integration.tag.GitHubTagCause

import lib.FormTagLib

def f = namespace(FormTagLib);

if (my.htmlUrl != null) {
    text("GitHub tag ")
    a(href: "${my.htmlUrl}") {
        text("${my.tagName}")
    }
    text(": ${my.reason}")
} else{
    text("Tag deleted")
}
