package org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Labels"), field:"labels"){
    f.expandableTextbox()
}
