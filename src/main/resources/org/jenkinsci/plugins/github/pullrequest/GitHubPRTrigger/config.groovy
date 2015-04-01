package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

def f = namespace(lib.FormTagLib);

def events = (instance == null ? [] : instance.events)
f.entry(title:_("Github hooks"), field:"useGitHubHooks") {
    f.checkbox()
}

f.entry(title:_("Crontab line"), field: "spec", help:"/descriptor/hudson.triggers.TimerTrigger/help/spec")  {
    f.textbox(default: descriptor.spec, checkUrl: "'descriptorByName/hudson.triggers.TimerTrigger/checkSpec?value=' + encodeURIComponent(this.value)")
}

f.entry(title: "Set status before build", field: "preStatus") {
    f.checkbox()
}

f.entry(title: "Cancel previous builds", field: "cancelPrev") {
    f.checkbox()
}

f.entry(title: "Skip first run", field: "skipFirstRun") {
    f.checkbox()
}

f.entry(title:_("Trigger Events")) {
    f.hetero_list(name: "events",
            items: events,
            descriptors: descriptor.getEventDescriptors(),
            hasHeader: true
    )
}

f.entry() {
    f.optionalProperty(title: "User Restriction", field: "userRestriction")
}

f.optionalProperty(title: "Branch Restriction", field: "branchRestriction")
