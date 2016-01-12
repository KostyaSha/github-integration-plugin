package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher.BuildMessage

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Build success message")) {
    f.property(field: "successMsg")
}

f.entry(title: _("Build failure message")) {
    f.property(field: "failureMsg")
}
