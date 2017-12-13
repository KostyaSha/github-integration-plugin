package com.github.kostyasha.github.integration.multibranch.GitHubSCMSource

import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandlerDescriptor
import lib.FormTagLib

import static com.github.kostyasha.github.integration.multibranch.repoprovider.GitHubRepoProvider2.GitHubRepoProviderDescriptor2.allRepoProviders2


def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubSCMSource();
}

f.block() {
    f.invisibleEntry() {
        f.textbox(field: "id", name: "id", value: instance.id)
    }

    f.entry(field: 'projectUrlStr', title: _('github.project.url')) {
        f.textbox()
    }

    f.dropdownList(name: "repoProvider", title: _("Repo Provider")) {
        allRepoProviders2().each { sd ->
            if (sd != null) {
                f.dropdownListBlock(value: sd.clazz.name, name: sd.displayName,
                        selected: instance.repoProvider == null ?
                                false : instance.repoProvider.descriptor.equals(sd),
                        title: sd.displayName) {
                    descriptor = sd
                    if (instance.repoProvider != null && instance.repoProvider.descriptor.equals(sd)) {
                        instance = instance.repoProvider
                    }
                    f.invisibleEntry() {
                        input(type: "hidden", name: "stapler-class", value: sd.clazz.name)
                    }
                    st.include(from: sd, page: sd.configPage, optional: "true")
                }
            }
        }
    }

    f.entry(title: "Handlers") {
        f.hetero_list(
                name: "handlers",
                items: instance.handlers,
                descriptors: GitHubHandlerDescriptor.getAllGitHubHandlerDescriptors(),
                hasHeader: true,
                addCaption: _("Add Handler")
        )
    }
}
