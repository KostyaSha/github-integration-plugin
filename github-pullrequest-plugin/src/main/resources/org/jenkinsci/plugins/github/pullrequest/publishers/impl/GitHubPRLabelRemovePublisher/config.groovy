package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRLabelRemovePublisher

def f = namespace(lib.FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

//f.entry(title:_("Labels to remove")) {
f.property(field:"labelProperty")
//}

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
