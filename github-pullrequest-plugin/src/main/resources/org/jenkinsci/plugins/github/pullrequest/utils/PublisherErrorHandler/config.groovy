package org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler;

def f = namespace(lib.FormTagLib);

f.entry(title:_("If publisher failed mark build as "), field:"buildStatus"){
    f.select()
}
