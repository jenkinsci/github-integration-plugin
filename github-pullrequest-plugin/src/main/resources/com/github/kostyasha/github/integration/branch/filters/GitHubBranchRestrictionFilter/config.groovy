package com.github.kostyasha.github.integration.branch.trigger.filters.GitHubBranchRestrictionFilter

import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib);
def f = namespace(FormTagLib);
def j = namespace("jelly:core");

f.entry(field: "exclude", title: "Exclude branches that match criteria?") {
    f.checkbox()
}

f.entry(field: "matchAsPattern", title: "Match branches against pattern criteria?") {
    f.checkbox()
}

f.entry(title: "Branch Names or Patterns") {
    f.textarea(field: "matchCriteria")
}
