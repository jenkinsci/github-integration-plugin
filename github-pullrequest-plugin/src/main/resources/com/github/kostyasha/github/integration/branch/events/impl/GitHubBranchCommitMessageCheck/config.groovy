package com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitMessageCheck

import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

f.entry(field: "exclude", title: "Exclude commits that match criteria?") {
    f.checkbox()
}

f.entry(title: _("Message Patterns"), field: "matchCriteria") {
    f.expandableTextbox()
}
