package org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder

import lib.FormTagLib
import org.jenkinsci.plugins.github.pullrequest.builders.GitHubPRStatusBuilder

def f = namespace(FormTagLib);

// Stapler doesn't produce default values, so recreate object
if (instance == null) {
    instance = new GitHubPRStatusBuilder()
}

f.entry(title: _("Build status message")) {
    f.property(field: "statusMessage")
}
f.optionalProperty(title: "Use custom status identifier name", field: "customCheck")
