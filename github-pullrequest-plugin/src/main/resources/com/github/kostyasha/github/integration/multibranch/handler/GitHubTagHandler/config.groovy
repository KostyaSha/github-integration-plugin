package com.github.kostyasha.github.integration.multibranch.handler.GitHubTagHandler

import com.github.kostyasha.github.integration.tag.events.GitHubTagEventDescriptor
import com.github.kostyasha.github.integration.multibranch.handler.GitHubTagHandler
import lib.FormTagLib

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubTagHandler();
}

f.entry(title: _("Trigger Events"), help: descriptor.getHelpFile('events')) {
    f.hetero_list(name: "events",
            items: instance.events,
            descriptors: GitHubTagEventDescriptor.all(),
            hasHeader: true
    )
}
