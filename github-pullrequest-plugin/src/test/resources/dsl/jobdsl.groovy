freeStyleJob('gh-pull-request') {

    triggers {
        onPullRequest {
            setPreStatus()
            mode {
                cron()
                heavyHooks()
                heavyHooksCron()
            }
            events {
                opened()
                closed()
                commented("comment text")
                labelAdded("foo")
                labelExists("bar")
                skipLabelExists("bar")

                labelNotExists("jenkins")
                skipLabelNotExists("jenkins")

                labelsMatchPattern("pattern")
                skipLabelsMatchPattern("pattern")

                labelRemoved('name')

                nonMergeable()
                skipNonMergeable()

                skipDescription("skip message")
                commit()
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
