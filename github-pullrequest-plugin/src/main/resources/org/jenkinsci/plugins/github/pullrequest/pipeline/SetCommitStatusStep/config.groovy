package org.jenkinsci.plugins.github.pullrequest.pipeline.SetCommitStatusStep

import lib.FormTagLib;

def f = namespace(FormTagLib);


f.entry(title: _("Status context"), field: "context") {
    f.textbox()
}

f.entry(title: _("Status"), field: "state") {
    f.enum() {
        text(my.name().toLowerCase().capitalize())
    }
}

f.entry(title: _("Status message"), field: "message") {
    f.textarea()
}
