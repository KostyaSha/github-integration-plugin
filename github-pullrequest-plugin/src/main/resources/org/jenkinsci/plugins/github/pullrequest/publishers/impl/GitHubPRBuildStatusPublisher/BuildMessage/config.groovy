package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher.BuildMessage;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Build success message")){
    f.property(field:"successMsg")
}

f.entry(title:_("Build failure message")){
    f.property(field:"failureMsg")
}
