package com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler

import com.github.kostyasha.github.integration.branch.events.GitHubBranchEventDescriptor
import com.github.kostyasha.github.integration.multibranch.handler.GitHubBranchHandler
import lib.FormTagLib

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubBranchHandler();
}

f.entry(title: _("Trigger Events"), help: descriptor.getHelpFile('events')) {
    f.hetero_list(name: "events",
            items: instance.events,
            descriptors: GitHubBranchEventDescriptor.all(),
            hasHeader: true
    )
}
