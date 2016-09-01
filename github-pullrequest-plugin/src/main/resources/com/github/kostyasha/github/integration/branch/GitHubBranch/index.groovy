package com.github.kostyasha.github.integration.branch.GitHubBranch

import com.google.common.base.Joiner

a(href: my.branchName) {
    img(src: rootURL + my.iconFileName, width: "16", height: "16")
    text(" #" + my.branchName + ": " + my.branchName)
}

table(width: "1000px") {
    tr() {
        td("Branch: " + my.branchName)
    }
//    tr() {
//        td("Head SHA: " + my.headSha)
//        td("Author: " + my.userLogin)
//        td("Issue updated at " + my.issueUpdatedAt)
//    }
//    tr() {
//        td("Source branch: " + my.headRef)
//        td("Author's email: " + my.userEmail)
//        td("PR updated at " + my.prUpdatedAt)
//    }
//    tr() {
//        td("Target branch: " + my.baseRef)
//        td(my.mergeable ? "PR mergeable " : "PR NOT mergeable")
//        td("Last commented at " + my.lastCommentCreatedAt)
//    }
//    tr() {
//        def labels = (my.labels) ? Joiner.on(", ").join((List) my.labels.collect { x -> "\"" + x + "\"" }) : "none"
//        td(colspan: 3, "Labels: " + labels)
//    }
}
