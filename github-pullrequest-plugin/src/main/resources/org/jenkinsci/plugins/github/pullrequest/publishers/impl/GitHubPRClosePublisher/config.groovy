package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRClosePublisher;

def f = namespace(lib.FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
