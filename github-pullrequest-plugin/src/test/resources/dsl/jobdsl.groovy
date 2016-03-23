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
            }
        }
    }

    publishers {
        commitStatusOnGH {
            unstableAsError()
            message('Build finished')
        }
    }
}
