package org.jenkinsci.plugins.github.pullrequest.GitHubPRCheckName

import lib.FormTagLib

def f = namespace(FormTagLib);

f.entry(title: _("Status check name"), field: "checkName", description: "Optional") {
    f.textbox()
}
