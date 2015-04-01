package org.jenkinsci.plugins.github.pullrequest.GitHubPRPullRequest

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");

a(href:my.htmlUrl, "#" + my.number + ": " + my.title)

table(width:"900px") {
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
        def labels = my.labels ? my.labels : "none"
        td(colspan:3, "Has labels: " + labels)
    }
}
