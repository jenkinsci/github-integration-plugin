package org.jenkinsci.plugins.github_integration.its.WorkflowITest


try {
    node('master') {
        step([
                $class       : 'GitHubPRStatusBuilder',
                statusMessage: [
                        content: "Run #${env.BUILD_NUMBER} started"
                ]
        ])
        setGitHubPullRequestStatus state: 'PENDING', context: 'custom-context1', message: "Run #${env.BUILD_NUMBER} started"
        setGitHubPullRequestStatus state: 'PENDING', context: 'custom-context2', message: "Run #${env.BUILD_NUMBER} started"

        sh 'sleep 10 && env'

        setGitHubPullRequestStatus state: 'SUCCESS', context: 'custom-context1', message: 'Tests passed'
        setGitHubPullRequestStatus state: 'SUCCESS', context: 'custom-context2', message: 'Tests passed'
        step([
                $class    : 'GitHubPRBuildStatusPublisher',
                statusMsg : [
                        content: 'Run #${BUILD_NUMBER} ended normally'
                ],
                unstableAs: 'FAILURE'
        ])
    }
} catch (Exception e) {
    setGitHubPullRequestStatus state: 'FAILURE', context: 'custom-context1', message: 'Some tests failed'
    setGitHubPullRequestStatus state: 'FAILURE', context: 'custom-context2', message: 'Some tests failed'
    throw e
}
