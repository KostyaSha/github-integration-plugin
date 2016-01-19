package org.jenkinsci.plugins.github.pullrequest.restrictions.GitHubPRUserRestriction;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Organizations"), field:"orgs"){
    f.textarea()
}

f.entry(title:_("Users"), field: "users"){
    f.textarea()
}

f.entry(title:_("Admins"), field: "adminList"){
    f.textarea()
}

//f.entry(title:_("List of organisations. Their members will be whitelisted"), field:"orgslist"){
//    f.textarea()
//}

f.entry(title: _("Whitelist user msg"), field: "whitelistUserMsg" ){
    f.textbox(default: ".*add\\W+to\\W+whitelist.*")
}

f.entry(title:_("Organisations members as admins"),
        field:"allowMembersOfWhitelistedOrgsAsAdmin"){
    f.checkbox()
}
