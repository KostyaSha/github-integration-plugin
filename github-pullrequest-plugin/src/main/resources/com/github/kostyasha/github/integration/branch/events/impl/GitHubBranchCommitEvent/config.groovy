package com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent

import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitEvent
import lib.FormTagLib

def f = namespace(FormTagLib)

if (instance == null) {
    instance = new GitHubBranchCommitEvent()
}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry() {
            f.hetero_list(name: "checks",
                    items: instance.checks,
                    descriptors: descriptor.getEventDescriptors(),
                    hasHeader: true
            )
        }
    }
}
