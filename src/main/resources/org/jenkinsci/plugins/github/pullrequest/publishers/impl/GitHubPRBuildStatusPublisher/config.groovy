package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;

def f = namespace(lib.FormTagLib);

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

f.entry(title:_("Status message")) {
    f.property(field: "statusMsg")
}

f.entry(title:_("Mark unstable build in GitHub as "), field:"unstableAs"){
    f.enum(){
        text(my.name())
    }
}

f.optionalProperty(title: "Use messages in case of status setting failure", field: "buildMessage")

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")
