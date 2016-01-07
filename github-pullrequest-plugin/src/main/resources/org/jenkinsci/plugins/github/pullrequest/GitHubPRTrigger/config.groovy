package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

def f = namespace(lib.FormTagLib);

def events = (instance == null ? [] : instance.events)
//f.entry(title:_("Github hooks"), field:"useGitHubHooks") {
//    f.checkbox()
//}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry(title: "Trigger Mode", field: "triggerMode") {
            f.enum() {
                text(my.description)
            }
        }

        f.entry(title: _("Crontab line"), field: "spec", help: "/descriptor/hudson.triggers.TimerTrigger/help/spec") {
            f.textbox(default: descriptor.spec, checkUrl: "'descriptorByName/hudson.triggers.TimerTrigger/checkSpec?value=' + encodeURIComponent(this.value)")
        }

        f.entry(title: "Set status before build", field: "preStatus") {
            f.checkbox()
        }

        f.entry(title: "Cancel queued builds", field: "cancelQueued") {
            f.checkbox()
        }

        f.entry(title: "Skip first run", field: "skipFirstRun") {
            f.checkbox()
        }

        f.entry(title: _("Trigger Events"), help: descriptor.getHelpFile('events')) {
            f.hetero_list(name: "events",
                    items: events,
                    descriptors: descriptor.getEventDescriptors(),
                    hasHeader: true
            )
        }

        f.optionalProperty(title: "User Restriction", field: "userRestriction")
   
        f.optionalProperty(title: "Branch Restriction", field: "branchRestriction")
    }
}
