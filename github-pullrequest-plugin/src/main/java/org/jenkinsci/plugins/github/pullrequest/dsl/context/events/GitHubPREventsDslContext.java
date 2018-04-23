package org.jenkinsci.plugins.github.pullrequest.dsl.context.events;

import javaposse.jobdsl.dsl.Context;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRLabel;
import org.jenkinsci.plugins.github.pullrequest.events.GitHubPREvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCloseEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommentEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRCommitEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRDescriptionEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelAddedEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelNotExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelPatternExistsEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRLabelRemovedEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNonMergeableEvent;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRNumber;
import org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPROpenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPREventsDslContext implements Context {
    private List<GitHubPREvent> events = new ArrayList<>();

    public void closed() {
        events.add(new GitHubPRCloseEvent());
    }

    public void opened() {
        events.add(new GitHubPROpenEvent());
    }

    public void commit() {
        events.add(new GitHubPRCommitEvent());
    }

    public void commented(String comment) {
        events.add(new GitHubPRCommentEvent(comment));
    }

    public void skipDescription(String skipMsg) {
        events.add(new GitHubPRDescriptionEvent(skipMsg));
    }

    public void labelAdded(String label) {
        events.add(new GitHubPRLabelAddedEvent(new GitHubPRLabel(label)));
    }

    public void labelExists(String label) {
        events.add(new GitHubPRLabelExistsEvent(new GitHubPRLabel(label), false));
    }

    public void skipLabelExists(String label) {
        events.add(new GitHubPRLabelExistsEvent(new GitHubPRLabel(label), true));
    }

    public void labelNotExists(String label) {
        events.add(new GitHubPRLabelNotExistsEvent(new GitHubPRLabel(label), false));
    }

    public void skipLabelNotExists(String label) {
        events.add(new GitHubPRLabelNotExistsEvent(new GitHubPRLabel(label), true));
    }

    public void labelMatchPattern(String pattern) {
        events.add(new GitHubPRLabelPatternExistsEvent(new GitHubPRLabel(pattern), false));
    }

    public void skipLabelMatchPattern(String pattern) {
        events.add(new GitHubPRLabelPatternExistsEvent(new GitHubPRLabel(pattern), true));
    }

    public void labelRemoved(String label) {
        events.add(new GitHubPRLabelRemovedEvent(new GitHubPRLabel(label)));
    }

    public void nonMergeable() {
        events.add(new GitHubPRNonMergeableEvent(false));
    }

    public void skipNonMergeable() {
        events.add(new GitHubPRNonMergeableEvent(true));
    }

    public void number(int prNumber, boolean match) {
        events.add(new GitHubPRNumber(prNumber, match, false));
    }

    public void skipNumber(int prNumber, boolean match) {
        events.add(new GitHubPRNumber(prNumber, match, true));
    }

    public List<GitHubPREvent> events() {
        return events;
    }
}
