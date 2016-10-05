package com.github.kostyasha.github.integration.branch.test;

import org.junit.rules.ExternalResource;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Helpful class to make possible usage of
 * {@code @Inject
 * public GitHubPluginConfig config;
 * }
 * <p>
 * in test fields instead of static calls {@link org.jenkinsci.plugins.github.GitHubPlugin#configuration()}
 * <p>
 * See {@link com.cloudbees.jenkins.GitHubSetCommitStatusBuilderTest} for example
 * Should be used after JenkinsRule initialized
 * <p>
 * {@code public RuleChain chain = RuleChain.outerRule(jRule).around(new InjectJenkinsMembersRule(jRule, this)); }
 *
 * @author lanwen (Merkushev Kirill)
 */
public class InjectJenkinsMembersRule extends ExternalResource {

    private JenkinsRule jRule;
    private Object instance;

    /**
     * @param jRule    Jenkins rule
     * @param instance test class instance
     */
    public InjectJenkinsMembersRule(JenkinsRule jRule, Object instance) {
        this.jRule = jRule;
        this.instance = instance;
    }

    @Override
    protected void before() throws Throwable {
        jRule.getInstance().getInjector().injectMembers(instance);
    }
}
