package com.github.kostyasha.github.integration.generic.errors.GitHubErrorsAction

def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");

if (from?.hasVisibleErrors()) {
    // indent like in hudson/model/AbstractProject/main.jelly
    table(style: "margin-left:1em;") {
        t.summary(icon: "warning.png") {
            b(from.description)
            br()
            from.getErrorsSnapshot().eachWithIndex() { error, index ->
                if (error.isVisible()) {
                    b(++index + " " + error.title)
                    div(style: "margin-left:2em") {
                        raw(error.getHtmlDescription())
                    }
                    st.include(page: "index.jelly", it: error, optional: true)
                }
            }
        }
    }
}
