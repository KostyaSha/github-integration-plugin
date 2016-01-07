package org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier;

def f = namespace(lib.FormTagLib);

f.entry(title:_("Run when build status is better or equal"), field:"buildStatus"){
    f.select()
}
