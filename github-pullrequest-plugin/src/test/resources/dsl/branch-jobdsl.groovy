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

                branchRestriction
                {
                    matchCritieria('master')
                    matchCritieria('other')
                }

                commitChecks
                {
                    commitMessagePattern
                    {
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
