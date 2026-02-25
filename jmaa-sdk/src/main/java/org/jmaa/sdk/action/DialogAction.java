package org.jmaa.sdk.action;

import org.jmaa.sdk.Action;

public  class DialogAction implements Action {
    String model;
    String type;

    public DialogAction(String model, String type) {
        this.model = model;
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getAction() {
        return "dialog";
    }
}
