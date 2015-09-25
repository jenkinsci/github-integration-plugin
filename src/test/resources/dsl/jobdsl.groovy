freeStyleJob('gh-pull-request') {

    triggers {
        onPullRequest {
            setPreStatus()
            mode {
                heavyHooks()
            }
            events {
                opened()
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