package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider
import hudson.triggers.TimerTrigger
import lib.FormTagLib
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubPRTrigger();
}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry(title: "Trigger Mode", field: "triggerMode") {
            f.enum() {
                text(my.description)
            }
        }

        f.entry(title: _("Crontab line"), field: "spec", help: "/descriptor/${TimerTrigger.getClass().getName()}/help/spec") {
            f.textbox(default: "H/5 * * * *",
                    checkUrl: "'descriptorByName/hudson.triggers.TimerTrigger/checkSpec?value=' + encodeURIComponent(this.value)")
        }

        f.entry(title: "Set status before build", field: "preStatus") {
            f.checkbox()
        }

        f.entry(title: "Cancel queued builds", field: "cancelQueued") {
            f.checkbox()
        }

        f.entry(title: "Abort running builds", field: "abortRunning") {
            f.checkbox()
        }

        f.entry(title: "Skip older PRs on first run", field: "skipFirstRun") {
            f.checkbox()
        }

        f.entry(title: _("Trigger Events"), help: descriptor.getHelpFile('events')) {
            f.hetero_list(name: "events",
                    items: instance.events,
                    descriptors: descriptor.getEventDescriptors(),
                    hasHeader: true
            )
        }

        f.optionalProperty(title: "Experimental: User Restriction", field: "userRestriction")

        f.optionalProperty(title: "Experimental: Branch Restriction", field: "branchRestriction")

        f.advanced(title: "Experimental") {
            f.dropdownList(name: "repoProvider", title: _("GitHub Connection Repo Provider")) {
                GitHubRepoProvider.GitHubRepoProviderDescriptor.allRepoProviders().each() { d ->
                    if (d != null) {

                        f.dropdownListBlock(
                                value: d.clazz.name,
                                name: d.displayName,
                                selected: instance.repoProvider == null ?
                                        false : instance.repoProvider.descriptor.equals(d),
                                title: d.displayName) {
                            descriptor = d
                            if (instance.repoProvider != null && instance.repoProvider.descriptor.equals(d)) {
                                instance = instance.repoProvider
                            }
                            f.invisibleEntry() {
                                input(type: "hidden", name: "stapler-class", value: d.clazz.name)
                            }
                            st.include(from: d, page: d.configPage, optional: "true")
                        }
                    }
                }
            }
        }
    }
}
