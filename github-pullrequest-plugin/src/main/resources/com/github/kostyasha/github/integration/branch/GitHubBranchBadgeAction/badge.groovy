package com.github.kostyasha.github.integration.branch.GitHubBranchBadgeAction

import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

a(href: my.htmlUrl) {
    img(src: "${rootURL}${h.getResourcePath()}/plugin/github-pullrequest/git-branch.svg",
            title: my.title,
            width: "16",
            height: "16"
    )
    text("${my.branchName}")
}
