package org.jmaa.sdk.action;

import org.jmaa.sdk.Action;

public  class ServiceAction implements Action {
    String model;
    String service;

    public ServiceAction(String model, String service) {
        this.model = model;
        this.service = service;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String getAction() {
        return "service";
    }
}
