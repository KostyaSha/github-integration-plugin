package org.jenkinsci.plugins.github.pullrequest.utils.PublisherErrorHandler

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("If publisher failed mark build as "), field: "buildStatus") {
    f.select()
}
