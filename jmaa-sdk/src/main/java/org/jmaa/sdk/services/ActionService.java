package org.jmaa.sdk.services;

import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Service;
import org.jmaa.sdk.exceptions.AccessException;

import java.util.List;
import java.util.Map;

/**
 * @author Eric Liang
 */
public class ActionService extends Service {
    @Override
    @SuppressWarnings("unchecked")
    protected void execute(ParamIn in, ParamOut out) {
        ActionParam param = in.getArgs(ActionParam.class);
        Records rec = in.getEnv().get(in.getModel(), param.ids);
        rec.load(param.values);
        if (!rec.getMeta().getActions().contains(param.action)) {
            throw new AccessException(String.format("模型%s没有动作%s", in.getModel(), param.action));
        }
        Object result = rec.call(param.action);
        out.putData(result);
        in.getEnv().getCursor().rollback();
    }
}

class ActionParam {
    public List<String> ids;
    public Map<String, Object> values;
    public String action;
}
