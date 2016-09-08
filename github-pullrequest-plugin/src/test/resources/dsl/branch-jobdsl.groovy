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
            events {
                created()
                hashChanged()
                deleted()
            }
        }

    }
}
