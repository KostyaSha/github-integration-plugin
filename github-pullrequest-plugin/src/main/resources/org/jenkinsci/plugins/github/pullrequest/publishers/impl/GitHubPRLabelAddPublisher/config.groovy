package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRLabelAddPublisher

import lib.FormTagLib

def f = namespace(FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

f.entry(title: _("Labels to add")) {
    f.property(field: "labelProperty")
}

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
