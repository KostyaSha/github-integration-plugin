package org.jenkinsci.plugins.github.pullrequest.webhook;

/**
 * Bean class to store main info from payload of webhook
 *
 * @author lanwen (Merkushev Kirill)
 */
public class PullRequestInfo {
    private final String repo;
    private final int num;

    public PullRequestInfo(String repo, int num) {
        this.repo = repo;
        this.num = num;
    }

    public String getRepo() {
        return repo;
    }

    public int getNum() {
        return num;
    }
}
