package org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest

import com.google.common.base.Joiner
import hudson.Functions
import jenkins.model.Jenkins

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");

img(src: Jenkins.instance.rootUrl + my.iconFileName, width:"16", height:"16")
a(href:my.htmlUrl, "#" + my.number + ": " + my.title)

table(width:"1000px") {
    tr() {
        td("Head SHA: " + my.headSha)
        td("Author: " + my.userLogin)
        td("Issue updated at " + my.issueUpdatedAt)
    }
    tr() {
        td("Source branch: " + my.headRef)
        td("Author's email: " + my.userEmail)
        td("PR updated at " + my.prUpdatedAt)
    }
    tr(){
        td("Target branch: " + my.baseRef)
        td("Is mergeable? " + my.mergeable)
        td("Last commented at " + my.lastCommentCreatedAt)
    }
    tr() {
        def labels = (my.labels) ? Joiner.on(", ").join((List)my.labels.collect {x ->"\"" + x + "\""}) : "none"
        td(colspan:3, "Labels: " + labels)
    }
}
