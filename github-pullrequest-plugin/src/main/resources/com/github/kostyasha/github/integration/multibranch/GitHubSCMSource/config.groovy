package com.github.kostyasha.github.integration.multibranch.GitHubSCMSource

import com.github.kostyasha.github.integration.generic.GitHubRepoProvider
import com.github.kostyasha.github.integration.multibranch.GitHubSCMSource
import com.github.kostyasha.github.integration.multibranch.handler.GitHubHandlerDescriptor
import jenkins.plugins.git.GitSCMSource
import lib.FormTagLib


def f = namespace(FormTagLib);
def st = namespace("jelly:stapler")

if (instance == null) {
    instance = new GitHubSCMSource();
}

f.block() {
    f.entry(field: 'projectUrlStr', title: _('github.project.url')) {
        f.textbox()
    }

    f.entry(title: "Repo providers") {
        f.hetero_list(name: "repoProviders",
                items: instance.repoProviders,
                descriptors: GitHubRepoProvider.GitHubRepoProviderDescriptor.allRepoProviders(),
                hasHeader: true

        )
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
