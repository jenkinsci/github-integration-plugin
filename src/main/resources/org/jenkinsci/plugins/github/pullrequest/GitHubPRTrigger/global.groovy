package org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger

def f = namespace(lib.FormTagLib);

f.section(title:_(descriptor.displayName)){
    f.entry(title: _("Published Jenkins URL"), field:"publishedURL"){
        f.textbox()
    }

    f.entry(title: _("Add user to white list phrase"), field:"whitelistUserMsg"){
        f.textbox(default: ".*ok\\W+to\\W+test.*")
    }

    f.entry(title: _("Crontab line"), field: "spec", help: "/descriptor/hudson.triggers.TimerTrigger/help/spec") {
        f.textbox(default:"H/5 * * * *", checkUrl: "'descriptorByName/hudson.triggers.TimerTrigger/checkSpec?value=' + encodeURIComponent(this.value)")
    }
}