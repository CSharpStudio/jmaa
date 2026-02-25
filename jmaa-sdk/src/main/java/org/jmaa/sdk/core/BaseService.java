package org.jmaa.sdk.core;

import java.util.Collections;
import java.util.Map;

import org.jmaa.sdk.tools.DocUtils;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;

/**
 * 服务基类
 *
 * @author Eric Liang
 */
public class BaseService {
    String name;
    String description;
    String label;
    String auth;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAuth() {
        if (StringUtils.isEmpty(auth)) {
            auth = name;
        }
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public Map<String, ApiDoc> getArgsDoc(MetaModel meta) {
        return Collections.emptyMap();
    }

    public ApiDoc getResultDoc(MetaModel meta) {
        return null;
    }

    protected void execute(ParamIn in, ParamOut out) {

    }

    protected void onError(ParamIn in, ParamOut out) {

    }

    public void executeService(ParamIn in, ParamOut out) {

        if (out.getErrors().isEmpty()) {
            execute(in, out);
        } else {
            onError(in, out);
        }
    }

    public class ApiDoc {
        String type;
        Object example;
        String description;

        public ApiDoc(String description, Object example, String type) {
            this.type = type;
            this.description = description;
            this.example = example;
        }

        public ApiDoc(String description, Object example) {
            this.type = DocUtils.getJsonType(example.getClass());
            this.description = description;
            this.example = example;
        }

        public String getType() {
            return type;
        }

        public Object getExample() {
            return example;
        }

        public String getDescription() {
            return description;
        }
    }
}
