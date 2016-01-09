package org.jenkinsci.plugins.github_integration;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithPlugin;

import java.io.IOException;

/**
 * @author Kanstantsin Shautsou
 */
public class SomeTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

//    @WithPlugin(value = )
    @Test
    public void someTest() throws IOException {
        j.createFreeStyleProject("dsf");
        j.pause();
    }
}
