
package org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository

import hudson.model.Item
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");
def makeBuildItem(def builds) {
    a("Related builds: ")
    for (build in builds) {
        a(href: rootURL + "/" + build.url + "console/") {
            img(src: rootURL + "/images/16x16/" + build.buildStatusUrl)
        }
        a(href: rootURL + "/" + build.url, build.displayName, title:build.getCause(GitHubPRCause.class).reason)
        text(" ")
    }
}


l.layout(title: "GitHub Pull Request Status") {
    st.include(page: "sidepanel", it: my.job)
    script(src:"${rootURL}${h.getResourcePath()}/plugin/github-pullrequest/scripts/featureButton.js")
    l.main_panel() {
        h1("GitHub Pull Request Status");
        text("Repository: ")
        a(href:my.githubUrl, my.fullName)

        br()
        br()
        div(style: "display: inline-block") {
            if (h.hasPermission(Item.BUILD)) {
                def runTrigger = "runTrigger";
                form(method: "post", action: "runTrigger", onsubmit: "return callFeature(this, ${runTrigger})",
                        style: "float: right; margin-right: 100px") {
                    f.submit(value: _("Run GH PR Trigger"))
                    div(id: runTrigger)
                }
            }
        }

        def buildMap = my.getAllPrBuilds()
        table() {
            for (pr in my.pulls.values()) {
                tr() {
                    td() {
                        br()
                        st.include(page: "index", it: pr)
                    }
                }
                tr() {
                    td() { makeBuildItem(buildMap.get(pr.number)) }
                }
                if (h.hasPermission(Item.BUILD)) {
                    tr() {
                        td() {
                            def rebuildId = "rebuildResult" + pr.number;
                            form(method: "post", action: "rebuild",
                                    onsubmit: "return callFeature(this, ${rebuildId}, {'prNumber' : ${pr.number} })") {
                                f.submit(value: _("Rebuild"))
                                div(id: rebuildId)
                            }
                        }
                    }
                }
            }
        }
        br()

        div(style: "display: inline-block") {
            if (h.hasPermission(Item.BUILD)) {
                def rebuildFailedId = "rebuildFailedResult";
                form(method: "post", action: "rebuildFailed", onsubmit: "return callFeature(this, ${rebuildFailedId})",
                        style: "float: right; margin-right: 100px") {
                    f.submit(value: _("Rebuild all failed builds"))
                    div(id: rebuildFailedId)
                }
            }

            if (h.hasPermission(Item.DELETE)) {
                def clearRepoId = "clearRepoResult";
                form(method: "post", action: "clearRepo", onsubmit: "return callFeature(this, ${clearRepoId})",
                        style: "float: left") {
                    f.submit(value: _("Remove all repo data"))
                    div(id: clearRepoId)
                }
            }
        }
        br()
    }
}
