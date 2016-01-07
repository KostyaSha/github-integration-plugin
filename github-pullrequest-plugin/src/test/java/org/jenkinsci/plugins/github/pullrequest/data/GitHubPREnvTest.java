package org.jenkinsci.plugins.github.pullrequest.data;

import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPREnvTest {

    @Test
    public void shouldAddPrefixToStringVars() throws Exception {
         assertThat(GitHubPREnv.values()[0].param("value").getName(), startsWith(GitHubPREnv.PREFIX));
    }

    @Test
    public void shouldAddPrefixToBoolVars() throws Exception {
         assertThat(GitHubPREnv.values()[0].param(true).getName(), startsWith(GitHubPREnv.PREFIX));
    }
}
