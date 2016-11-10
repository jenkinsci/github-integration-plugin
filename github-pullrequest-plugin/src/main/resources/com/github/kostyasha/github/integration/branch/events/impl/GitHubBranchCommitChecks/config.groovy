package com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitChecks

import com.github.kostyasha.github.integration.branch.events.impl.GitHubBranchCommitChecks

import groovy.swing.factory.TitledBorderFactory
import lib.FormTagLib
import lib.LayoutTagLib

def l = namespace(LayoutTagLib)
def f = namespace(FormTagLib)
def j = namespace("jelly:core")

if (instance == null) {
    instance = new GitHubBranchCommitChecks()
}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry() {
            f.hetero_list(name: "events",
                    items: instance.events,
                    descriptors: descriptor.getEventDescriptors(),
                    hasHeader: true
            )
        }
    }
}
