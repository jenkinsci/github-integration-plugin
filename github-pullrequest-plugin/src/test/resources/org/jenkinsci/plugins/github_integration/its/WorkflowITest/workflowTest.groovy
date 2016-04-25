package org.jenkinsci.plugins.github_integration.its.WorkflowITest

node('master') {
    step([
            $class       : 'GitHubPRStatusBuilder',
            statusMessage: [
                    content: "Build #${env.BUILD_NUMBER} started"
            ]
    ])
//
//    checkout([
//            $class: 'GitSCM',
//            branches: [[name: "origin-pull/pull/${GITHUB_PR_NUMBER}/merge"]],
//            doGenerateSubmoduleConfigurations: false,
//            extensions: [],
//            submoduleCfg: [],
//            userRemoteConfigs: [
//                    [
//                            credentialsId: 'df5e384b-e836-42a0-b5cc-445e88ac6700',
//                            name: 'origin-pull',
//                            refspec: "+refs/pull/${GITHUB_PR_NUMBER}/merge:refs/remotes/origin-pull/pull/${GITHUB_PR_NUMBER}/merge",
//                            url: 'git://github.com/KostyaSha/test-repo.git'
//                    ]
//            ]
//    ])

    sh 'sleep 10 && env'
    step([
            $class    : 'GitHubPRBuildStatusPublisher',
            statusMsg : [
                    content: 'Build #${BUILD_NUMBER} ended'
            ],
            unstableAs: 'FAILURE'
    ])
}
