package org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRBranchRestriction

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Whitelist Target Branches:"), field: "targetBranch") {
    f.expandableTextbox()
}
