package com.github.kostyasha.github.integration.branch.GitHubBranch

a(href: my.htmlUrl) {
    img(src: rootURL + my.iconFileName, width: "16", height: "16")
    text(my.name)
}

table(width: "1000px") {
    tr() {
        td("SHA1: " + my.sha1)
    }
}
