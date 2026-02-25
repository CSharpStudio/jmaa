package org.jmaa.sdk.action;

import org.jmaa.sdk.Action;
import org.jmaa.sdk.Records;

import java.util.HashMap;
import java.util.Map;

public class AttrAction implements Action {
    Map<String, Object> attrs = new HashMap<>();

    @Override
    public String getAction() {
        return "attr";
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public AttrAction setValue(String field, Object value) {
        Map<String, Object> data = (Map<String, Object>) attrs.computeIfAbsent(field, k -> new HashMap<String, Object>());
        if (value instanceof Records) {
            value = ((Records) value).getPresent();
        }
        data.put("value", value);
        return this;
    }

    public AttrAction setRequired(String field, boolean required) {
        Map<String, Object> data = (Map<String, Object>) attrs.computeIfAbsent(field, k -> new HashMap<String, Object>());
        data.put("required", required);
        return this;
    }

    public AttrAction setReadonly(String field, boolean readonly) {
        Map<String, Object> data = (Map<String, Object>) attrs.computeIfAbsent(field, k -> new HashMap<String, Object>());
        data.put("readonly", readonly);
        return this;
    }

    public AttrAction setVisible(String field, boolean visible) {
        Map<String, Object> data = (Map<String, Object>) attrs.computeIfAbsent(field, k -> new HashMap<String, Object>());
        data.put("visible", visible);
        return this;
    }

    public AttrAction setAttr(String field, String attr, Object value) {
        Map<String, Object> data = (Map<String, Object>) attrs.computeIfAbsent(field, k -> new HashMap<String, Object>());
        data.put(attr, value);
        return this;
    }
}
