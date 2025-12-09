package org.jenkinsci.plugins.github.pullrequest.GitHubPRRepository

import hudson.model.Item
import lib.FormTagLib
import lib.LayoutTagLib
import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause

def f = namespace(FormTagLib);
def l = namespace(LayoutTagLib);
def st = namespace("jelly:stapler");

def makeBuildItem(def builds) {
    if (builds != null && !builds.isEmpty()) {
        a("Related builds: ")
        for (build in builds) {
            a(href: rootURL + "/" + build.url + "console/") {
                img(src: rootURL + "/images/16x16/" + build.buildStatusUrl)
            }
            a(href: rootURL + "/" + build.url, build.displayName, title: build.getCause(GitHubPRCause.class).reason)
            text(" ")
        }
    } else {
        a("No related builds.")
    }
}


l.layout(title: "GitHub Pull Request Status") {
    st.include(page: "sidepanel", it: my.job)
    script(src: "${rootURL}${h.getResourcePath()}/plugin/github-pullrequest/scripts/featureButton.js")
    l.main_panel() {
        h1("GitHub Pull Request Status");
        text("Repository: ")
        a(href: my.githubUrl, my.fullName)

        br()
        br()
        div(style: "display: inline-block") {
            if (h.hasPermission(my.job, Item.BUILD)) {
                def runTrigger = "runTrigger";
                form(method: "post", action: "runTrigger", style: "float: right; margin-right: 100px",
                        class: "callFeature no-json",
                        'data-answerPlaceId': runTrigger,
                        'data-parameters': "{}") {
                    f.submit(value: _("Run GH PR Trigger"))
                    div(id: runTrigger)
                }
            }
        }

        def buildMap = my.getAllPrBuilds()
        table() {
            for (pr in my.pulls.values()) {
                def builds = buildMap.get(pr.number);
                tr() {
                    td() {
                        br()
                        st.include(page: "index", it: pr)
                    }
                }
                tr() {
                    td() { makeBuildItem(builds) }
                }


                if (h.hasPermission(my.job, Item.BUILD)) {
                    tr() {
                        td() {
                            div(style: "display: inline-block") {
                                // build local PR button
                                def buildResultId = "buildResult" + pr.number;
                                form(method: "post", action: "build",
                                        style: "float: left; ",
                                        class: "callFeature no-json",
                                        'data-answerPlaceId': buildResultId,
                                        'data-parameters': """{"prNumber" : "${pr.number}" }""") {
                                    f.submit(value: _("Build"))
                                    div(id: buildResultId) // some text from responce
                                }

                                // rebuild button
                                if (builds != null && !builds.isEmpty()) {
                                    def rebuildId = "rebuildResult" + pr.number;
                                    form(method: "post", action: "rebuild",
                                            style: "float: right; margin-right: 100px",
                                            class: "callFeature no-json",
                                            'data-answerPlaceId': rebuildId,
                                            'data-parameters': """{"prNumber" : "${pr.number}" }""") {
                                        f.submit(value: _("Rebuild last build"))
                                        div(id: rebuildId)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        br()
        div(style: "display: inline-block") {
            if (h.hasPermission(my.job, Item.BUILD)) {
                def rebuildAllFailedId = "rebuildFailedResult";
                form(method: "post",
                        name: "rebuildAllFailed",
                        action: "rebuildAllFailed",
                        style: "float: right; margin-right: 100px",
                        class: "callFeature no-json",
                        'data-answerPlaceId': rebuildAllFailedId,
                        'data-parameters': "{}") {
                    f.submit(value: _("Rebuild all failed builds"))
                    div(id: rebuildAllFailedId)
                }
            }

            if (h.hasPermission(my.job, Item.DELETE)) {
                def clearRepoId = "clearRepoResult";
                form(method: "post", action: "clearRepo",
                        style: "float: left", class: "callFeature no-json",
                        'data-answerPlaceId': clearRepoId, 'data-parameters': "{}") {
                    f.submit(value: _("Remove all repo data"))
                    div(id: clearRepoId)
                }
            }
        }
        br()
    }
}
