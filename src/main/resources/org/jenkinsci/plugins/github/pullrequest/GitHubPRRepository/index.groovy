package org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository

import jenkins.model.Jenkins
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");

def makeBuildItem(def builds) {
    a("Related builds: ")
    for (build in builds) {
        def rootUrl = Jenkins.instance.rootUrl
        a(href: rootUrl + build.url + "console/") {
            img(src: rootUrl + "/images/16x16/" + build.buildStatusUrl)
        }
        a(href: rootUrl + build.url, build.displayName, title:build.getCause(GitHubPRCause.class).reason)
        text(" ")
    }
    br()
    br()
}

l.layout(title: "GitHub Pull Requests statuses") {
    st.include(page: "sidepanel", it: my.project)
    l.main_panel() {
        h1("GitHub Pull Requests statuses");
        p("Repository: " + my.fullName)

        def buildMap = my.getAllPrBuilds()
        table() {
            for (pr in my.pulls.values()) {
                tr() {
                    td() { st.include(page: "index", it: pr) }
                }
                tr() {
                    td() { makeBuildItem(buildMap.get(pr.number)) }
                }
            }
        }
    }
}