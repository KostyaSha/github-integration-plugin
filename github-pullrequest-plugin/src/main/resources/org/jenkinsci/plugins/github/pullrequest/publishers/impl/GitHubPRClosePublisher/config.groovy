package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRClosePublisher

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
