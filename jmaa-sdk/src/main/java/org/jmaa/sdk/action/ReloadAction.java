package org.jmaa.sdk.action;

import org.jmaa.sdk.Action;

public class ReloadAction implements Action {
    String message;

    public String getMessage() {
        return message;
    }

    public Action setMessage(String message) {
        this.message = message;
        return this;
    }
    public ReloadAction(String message) {
        setMessage(message);
    }

    @Override
    public String getAction() {
        return "reload";
    }
}
