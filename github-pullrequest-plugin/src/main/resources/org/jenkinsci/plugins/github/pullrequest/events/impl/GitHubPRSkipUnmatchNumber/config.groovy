package org.jenkinsci.plugins.github.pullrequest.events.impl.GitHubPRSkipUnatchNumber

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(field: "number", title: "PR Number") {
    f.number()
}
