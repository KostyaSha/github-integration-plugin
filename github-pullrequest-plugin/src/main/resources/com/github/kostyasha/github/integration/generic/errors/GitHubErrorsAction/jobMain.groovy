package com.github.kostyasha.github.integration.generic.errors.GitHubErrorsAction

import com.github.kostyasha.github.integration.generic.errors.GitHubError
import lib.FormTagLib
import lib.LayoutTagLib

def f = namespace(FormTagLib);
def l = namespace(LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");
def j = namespace("jelly:core");

if (!from?.errors?.isEmpty()) {
    // indent like in hudson/model/AbstractProject/main.jelly
    table(style: "margin-left:1em;") {
        t.summary(icon: "warning.png") {
            b(from.description)
            br()
            from.getErrorsSnapshot().eachWithIndex() { error, index ->
                b(++index  + " " + error.title)
                div(style: "margin-left:2em") {
                    raw(error.getHtmlDescription())
                }
                st.include(page: "index.jelly", it: error, optional: true)
            }
        }
    }
}
