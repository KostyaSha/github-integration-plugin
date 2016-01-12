package org.jenkinsci.plugins.github.pullrequest.utils.StatusVerifier

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Run when build status is better or equal"), field: "buildStatus") {
    f.select()
}
