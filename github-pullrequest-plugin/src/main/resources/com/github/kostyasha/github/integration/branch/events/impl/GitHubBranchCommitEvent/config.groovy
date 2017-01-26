package com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent

import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent
import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib)
def f = namespace(FormTagLib)
def j = namespace("jelly:core")

if (instance == null) {
    instance = new GitHubBranchCommitEvent()
}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry() {
            f.hetero_list(name: "events",
                    items: instance.events,
                    descriptors: descriptor.getEventDescriptors(),
                    hasHeader: true
            )
        }
    }
}
