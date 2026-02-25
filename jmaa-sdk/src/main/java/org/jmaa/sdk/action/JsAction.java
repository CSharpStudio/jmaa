package org.jmaa.sdk.action;

import org.jmaa.sdk.Action;

public  class JsAction implements Action {
    String script;

    public JsAction(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @Override
    public String getAction() {
        return "js";
    }
}
