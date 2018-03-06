package com.github.kostyasha.github.integration.multibranch;

import java.io.IOException;

import com.github.kostyasha.github.integration.multibranch.head.GitHubSCMHead;
import com.github.kostyasha.github.integration.multibranch.revision.GitHubSCMRevision;

public interface SCMHeadConsumer {

    void accept(GitHubSCMHead head, GitHubSCMRevision revision) throws IOException, InterruptedException;

}
