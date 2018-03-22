package org.jenkinsci.plugins.github.pullrequest.webhook;

/**
 * Bean class to store main info from payload of webhook
 *
 * @author lanwen (Merkushev Kirill)
 */
public class PullRequestInfo {
    private final String repo;
    private final int num;
    private String target;

    public PullRequestInfo(String repo, int num) {
        this(repo, num, null);
    }

    public PullRequestInfo(String repo, int num, String target) {
        this.repo = repo;
        this.num = num;
        this.target = target;
    }

    public String getRepo() {
        return repo;
    }

    public int getNum() {
        return num;
    }

    public String getTarget() {
        return target;
    }
}
