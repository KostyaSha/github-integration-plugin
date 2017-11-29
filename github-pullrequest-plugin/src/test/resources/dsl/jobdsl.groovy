freeStyleJob('gh-pull-request') {

    triggers {
        onPullRequest {
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
                    permission { push() }
                }
            }

            events {
                opened()
                closed()
                commit()

                commented("match the comment")
                skipDescription("[skip ci]")

                labelAdded("jenkins")

                labelExists("build")
                skipLabelExists("skip-build")

                labelNotExists("skip-build")
                skipLabelNotExists("jenkins")

                labelMatchPattern("pattern")
                skipLabelMatchPattern("pattern")

                labelRemoved("skip-build")

                nonMergeable()
                skipNonMergeable()

                number(18, true)
                skipNumber(18, true)
            }
        }
    }

    steps {
        updateStatusOnGH {
            message('Building...')
        }
    }

    publishers {
        commitStatusOnGH {
            unstableAsError()
            message('Build finished')
        }

        commentPullRequestOnGH() {
            message("comment")
            onlyFailedBuilds()
            commentErrorIsFailure()
        }
    }
}
