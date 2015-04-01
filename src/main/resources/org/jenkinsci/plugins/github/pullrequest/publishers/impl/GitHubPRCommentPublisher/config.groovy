package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;

def f = namespace(lib.FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

//f.entry(title:_("Comment content"), field:"comment"){
f.property(field: "comment")
//}

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
