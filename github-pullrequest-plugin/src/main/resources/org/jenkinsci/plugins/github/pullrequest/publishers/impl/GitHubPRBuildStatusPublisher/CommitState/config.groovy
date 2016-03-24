package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher.BuildMessage

import lib.FormTagLib;

def f = namespace(FormTagLib);

f.entry(title: _("Commit status"), field: "state") {
    f.enum() {
        text(my.name().toLowerCase().capitalize())
    }
}
