package com.github.kostyasha.github.integration.branch.GitHubBranchRepository

import com.github.kostyasha.github.integration.branch.GitHubBranchCause
import hudson.model.Item
import groovy.json.StringEscapeUtils

def f = namespace(lib.FormTagLib);
def l = namespace(lib.LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");
def makeBuildItem(def runs) {
    a("Related builds: ")
    for (build in runs) {
        a(href: rootURL + "/" + build.url + "console/") {
            img(src: rootURL + "/images/16x16/" + build.buildStatusUrl)
        }
        a(href: rootURL + "/" + build.url, build.displayName, title:build.getCause(GitHubBranchCause.class).reason)
        text(" ")
    }
}

def escapeBranchName(def branchName) {
   // escape anything that isn't alphanumeric
   return StringEscapeUtils.escapeJavaScript(branchName);
}

def makeRebuildId(def branchName) {
    // replace anything that isn't alphanumeric so it's valid html
    return ("rebuildResult" + branchName).replaceAll(/([^a-zA-Z0-9])/, '');
}

l.layout(title: "GitHub Branch Status") {
    st.include(page: "sidepanel", it: my.job)
    script(src:"${rootURL}${h.getResourcePath()}/plugin/github-pullrequest/scripts/featureButton.js")
    l.main_panel() {
        h1("GitHub Branch Status");
        text("Repository: ")
        a(href:my.githubUrl, my.fullName)

        br()
        br()
        div(style: "display: inline-block") {
            if (h.hasPermission(Item.BUILD)) {
                def runTrigger = "runTrigger";
                form(method: "post", action: "runTrigger", onsubmit: "return callFeature(this, ${runTrigger})",
                        style: "float: right; margin-right: 100px") {
                    f.submit(value: _("Run Branch Trigger"))
                    div(id: runTrigger)
                }
            }
        }

        def buildMap = my.getAllBranchBuilds()
        table() {
            for (branch in my.branches.values()) {
                tr() {
                    td() {
                        br()
                        st.include(page: "index", it: branch)
                    }
                }
                tr() {
                    td() { makeBuildItem(buildMap.get(branch.name)) }
                }
                if (h.hasPermission(Item.BUILD)) {
                    tr() {
                        td() {
                            def rebuildId = makeRebuildId(branch.name);
                            def escaped = escapeBranchName(branch.name);
                            form(method: "post", action: "rebuild",
                                    onsubmit: "return callFeature(this, ${rebuildId}, {'branchName' : '${escaped}' })") {
                                f.submit(value: _("Rebuild"))
                                div(id: rebuildId)
                            }
                        }
                    }
                }
            }
        }
        br()

        div(style: "display: inline-block") {
            if (h.hasPermission(Item.BUILD)) {
                def rebuildFailedId = "rebuildFailedResult";
                form(method: "post", action: "rebuildFailed", onsubmit: "return callFeature(this, ${rebuildFailedId})",
                        style: "float: right; margin-right: 100px") {
                    f.submit(value: _("Rebuild all failed builds"))
                    div(id: rebuildFailedId)
                }
            }

            if (h.hasPermission(Item.DELETE)) {
                def clearRepoId = "clearRepoResult";
                form(method: "post", action: "clearRepo", onsubmit: "return  callFeature(this, ${clearRepoId})",
                        style: "float: left") {
                    f.submit(value: _("Remove all repo data"))
                    div(id: clearRepoId)
                }
            }
        }
        br()
    }
}
