package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher

import lib.FormTagLib;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRCommentPublisher;

def f = namespace(FormTagLib);

// Stapler doesn't produce default values, so recreate object
if (instance == null) {
    instance = new GitHubPRCommentPublisher();
}

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

//f.entry(title:_("Comment content"), field:"comment"){
f.property(field: "comment")
//}

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
