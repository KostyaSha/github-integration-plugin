package com.github.kostyasha.github.integration.multibranch.GitHubPRHandler

import com.github.kostyasha.github.integration.multibranch.handler.GitHubPRHandler
import lib.FormTagLib

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubPRHandler();
}

f.entry(title: _("Trigger Events"), help: descriptor.getHelpFile('events')) {
    f.hetero_list(name: "events",
            items: instance.events,
            descriptors: descriptor.getEventDescriptors(),
            hasHeader: true
    )
}
