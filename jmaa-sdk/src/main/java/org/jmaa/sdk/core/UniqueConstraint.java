package org.jmaa.sdk.core;

/**
 * @author Eric Liang
 */
public class UniqueConstraint {
    String name;
    String[] fields;
    String message;
    String module;
    boolean update;

    public UniqueConstraint(String name, String[] fields, String message, boolean update, String module) {
        this.name = name;
        this.fields = fields;
        this.message = message;
        this.module = module;
        this.update = update;
    }

    public String getName() {
        return name;
    }

    public String[] getFields() {
        return fields;
    }

    public String getMessage() {
        return message;
    }

    public String getModule() {
        return module;
    }

    public boolean isUpdate() {
        return update;
    }
}
