package org.jmaa.sdk.services;

import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eric Liang
 */
public class OnChangeService extends Service {
    @Override
    @SuppressWarnings("unchecked")
    protected void execute(ParamIn in, ParamOut out) {
        OnChangeParam param = in.getArgs(OnChangeParam.class);
        Records rec = in.getEnv().get(in.getModel(), param.ids);
        rec.load(param.values);
        Collection<String> methods = rec.getMeta().getOnChanges(param.field);
        Map<String, Object> result = new HashMap<>();
        for (String method : methods) {
            Map<String, Object> data = (Map<String, Object>) rec.call(method);
            result.putAll(data);
        }
        out.putData(result);
        in.getEnv().getCursor().rollback();
    }
}

class OnChangeParam {
    public List<String> ids;
    public Map<String, Object> values;
    public String field;
}
