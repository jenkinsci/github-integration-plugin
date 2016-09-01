package com.github.kostyasha.github.integration.branch.GitHubBranchPollingLogAction

import lib.FormTagLib
import lib.LayoutTagLib

def f = namespace(FormTagLib);
def l = namespace(LayoutTagLib);
def t = namespace("/lib/hudson")
def st = namespace("jelly:stapler");
def j = namespace("jelly:core");

def context = my.job ? my.job : my.run

l.layout(title: my.displayName) {
    st.include(page: "sidepanel", it: context)
    l.main_panel() {
        h1(my.displayName);
//        h4("Polling log of last attempt of build.")
        l.rightspace() {
            a(href: "pollingLog"){
                l.icon(class: "icon-document icon-md")
                text("View as plain text")
            }
        }

        if (my.logExists) {
            pre() {
                j.whitespace() {
                    my.writePollingLogTo(output);
                }
            }
        } else {
            text("There is no logs so far.");
        }
    }
}
