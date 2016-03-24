package org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;

import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher

def f = namespace(lib.FormTagLib);

// Stapler doesn't produce default values, so recreate object
if (instance == null) {
    instance = new GitHubPRBuildStatusPublisher();
}

f.optionalProperty(title: "Use only for builds with specific status", field: "statusVerifier")

f.entry(title: _("Status message")) {
    f.property(field: "statusMsg")
}

f.entry(title: _("Mark unstable build in GitHub as "), field: "unstableAs") {
    f.enum() {
        text(my.name())
    }
}

f.optionalProperty(title: "Use custom status identifier name", field: "customCheck")

f.optionalProperty(title: "Use messages in case of status setting failure", field: "buildMessage")

f.optionalProperty(title: "Handle publisher errors", field: "errorHandler")

f.optionalProperty(title: "Force specific status", field: "forceStatus") {
    f.enum() {
        text(my.name().toLowerCase().capitalize())
    }
}
