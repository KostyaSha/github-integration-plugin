
package org.jenkinsci.plugins.github.pullrequest.GitHubPRPollingLogAction

import org.jenkinsci.plugins.github.pullrequest.GitHubPRCause

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");
def j = namespace("jelly:core");

l.layout(title: my.displayName) {
    st.include(page: "sidepanel", it: my.project)
    l.main_panel() {
        h1(my.displayName);
//        h4("Polling log of last attempt of build.")
        def log = my.log;
        if (log) {j
            pre() {
                j.whitespace() {
                    my.writeLogTo(output);
                }
            }
        } else {
            text("There is no logs so far.");
        }
    }
}
