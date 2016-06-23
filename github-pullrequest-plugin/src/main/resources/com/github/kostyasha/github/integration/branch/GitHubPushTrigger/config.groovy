package com.github.kostyasha.github.integration.branch.GitHubPushTrigger

import com.cloudbees.jenkins.GitHubPushTrigger
import hudson.triggers.TimerTrigger
import lib.FormTagLib

def f = namespace(FormTagLib);

if (instance == null) {
    instance = new GitHubPushTrigger();
}

f.block {
    table(style: 'width:100%; margin-left: 5px;') {
        f.entry(title: "Trigger Mode", field: "triggerMode") {
            f.enum() {
                text(my.description)
            }
        }

        f.entry(title: _("Crontab line"), field: "spec", help: "/descriptor/${TimerTrigger.getClass().getName()}/help/spec") {
            f.textbox(default: "H/5 * * * *",
                    checkUrl: "'descriptorByName/hudson.triggers.TimerTrigger/checkSpec?value=' + encodeURIComponent(this.value)")
        }

        f.entry(title: "Set status before build", field: "preStatus") {
            f.checkbox()
        }

        f.entry(title: "Cancel queued builds", field: "cancelQueued") {
            f.checkbox()
        }

        f.entry(title: "Skip older PRs on first run", field: "skipFirstRun") {
            f.checkbox()
        }

        f.optionalProperty(title: "Experimental: User Restriction", field: "userRestriction")

        f.optionalProperty(title: "Experimental: Branch Restriction", field: "branchRestriction")
    }
}
