package org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Whitelist Target Branches:"), field:"targetBranch"){
    f.expandableTextbox()
}