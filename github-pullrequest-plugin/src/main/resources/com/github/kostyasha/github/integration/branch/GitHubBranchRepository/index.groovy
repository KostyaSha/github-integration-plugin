package com.github.kostyasha.github.integration.branch.GitHubBranchRepository

import com.github.kostyasha.github.integration.branch.GitHubBranchCause
import hudson.model.Item
import lib.FormTagLib
import lib.LayoutTagLib

import static org.apache.commons.lang3.StringEscapeUtils.escapeEcmaScript

def f = namespace(FormTagLib);
def l = namespace(LayoutTagLib);
def st = namespace("jelly:stapler");

def printRelatedBuilds(def runs) {
    if (runs != null && !runs.isEmpty()) {
        a("Related builds: ")
        for (build in runs) {
            a(href: rootURL + "/" + build.url + "console/") {
                img(src: rootURL + "/images/16x16/" + build.buildStatusUrl)
            }
            a(href: rootURL + "/" + build.url, build.displayName, title: build.getCause(GitHubBranchCause.class).reason)
            text(" ")
        }
    } else {
        a("No related builds.")
    }
}

static def makeRebuildResultId(def branchName) {
    // replace anything that isn't alphanumeric so it's valid html
    return ("rebuildResult" + branchName).replaceAll(/([^a-zA-Z0-9])/, '');
}

static def makeBuildResultId(def branchName) {
    // replace anything that isn't alphanumeric so it's valid html
    return ("buildResult" + branchName).replaceAll(/([^a-zA-Z0-9])/, '');
}

l.layout(title: "GitHub Branch Status") {
    st.include(page: "sidepanel", it: my.job)
    script(src: "${rootURL}${h.getResourcePath()}/plugin/github-pullrequest/scripts/featureButton.js")
    l.main_panel() {
        h1("GitHub Branch Status");
        text("Repository: ")
        a(href: my.githubUrl, my.fullName)
        br()
        br()
        div(style: "display: inline-block") {
            if (h.hasPermission(my.job, Item.BUILD)) {
                def runTrigger = "runTrigger";
                form(method: "post", action: "runTrigger",
                        style: "float: right; margin-right: 100px",
                        class: "callFeature no-json",
                        'data-answerPlaceId': runTrigger,
                        'data-parameters': "{}") {
                    f.submit(value: _("Run Branch Trigger"))
                    div(id: runTrigger)
                }
            }
        }

        def buildMap = my.getAllBranchBuilds()
        table() {
            for (branch in my.branches.values()) {
                def branchBuilds = buildMap.get(branch.name);
                tr() {
                    td() {
                        br()
                        // info about branch itself
                        st.include(page: "index", it: branch)
                    }
                }
                tr() {
                    td() { printRelatedBuilds(branchBuilds) }
                }
                // build local Branch button
                if (h.hasPermission(my.job, Item.BUILD)) {
                    tr() {
                        td() {
                            def buildResultId = makeBuildResultId(branch.name);
                            // escape anything that isn't alphanumeric
                            def escapedBuild = escapeEcmaScript(branch.name);
                            form(method: "post", action: "build",
                                    style: "float:left",
                                    class: "callFeature no-json",
                                    'data-answerPlaceId': buildResultId,
                                    'data-parameters': """{"branchName": "${escapedBuild}" }""") {
                                f.submit(value: _("Build"))
                                div(id: buildResultId) // some text from response
                            }

                            // rebuild button
                            if (branchBuilds != null && !branchBuilds.isEmpty()) {
                                def rebuildResultId = makeRebuildResultId(branch.name);
                                // escape anything that isn't alphanumeric
                                def escapedRebuild = escapeEcmaScript(branch.name);
                                form(method: "post",
                                        action: "rebuild",
                                        style: "float: left; margin-right: 100px",
                                        class: "callFeature no-json",
                                        'data-answerPlaceId': rebuildResultId,
                                        'data-parameters': """{"branchName": "${escapedRebuild}" }""") {
                                    f.submit(value: _("Rebuild last branch build"))
                                    div(id: rebuildResultId) // some text from response
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
                        style: "float: left",
                        class: "callFeature no-json",
                        'data-answerPlaceId': clearRepoId,
                        'data-parameters': "{}") {
                    f.submit(value: _("Remove all repo data"))
                    div(id: clearRepoId)
                }
            }
        }
        br()
    }
}
