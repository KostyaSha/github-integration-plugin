freeStyleJob('gh-branch') {

    triggers {
        onBranch {
            setPreStatus()
            cancelQueued()

            mode {
                cron()
                heavyHooks()
                heavyHooksCron()
            }

            repoProviders {
                gitHubPlugin {
                    manageHooks(false)
                    cacheConnection(false)
                    permission { pull() }
                }
            }

            events {

                branchRestriction {
                    matchCritieria('master')
                    matchCritieria('other')
                }

                commitChecks {
                    commitMessagePattern {
                        excludeMatching()
                        matchCritieria('^(?s)\\[(release|unleash)\\-maven\\-plugin\\].*')
                    }
                }

                created()
                hashChanged()
                deleted()
            }

            whitelistedBranches('master')
        }

    }
}
