package org.jmaa.sdk.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jmaa.sdk.Criteria;
import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.Service;
import org.jmaa.sdk.core.MetaModel;

/**
 * 统计模型服务
 *
 * @author Eric Liang
 */
public class CountService extends Service {
    @Override
    protected void execute(ParamIn in, ParamOut out) {
        CountParam param = in.getArgs(CountParam.class);
        long result = in.getEnv().get(in.getModel()).count(param.criteria);
        out.putData(result);
    }

    @Override
    public Map<String, ApiDoc> getArgsDoc(MetaModel meta) {
        Object example = meta.findField("name") == null ? Arrays.asList(Arrays.asList("id", "=", ""))
                : Arrays.asList(Arrays.asList("name", "=", "test"));
        return new HashMap<String, ApiDoc>(1) {
            {
                put("criteria", new ApiDoc("过滤条件", example));
            }
        };
    }

    @Override
    public ApiDoc getResultDoc(MetaModel meta) {
        return new ApiDoc("记录数", 10);
    }
}

class CountParam {
    public Criteria criteria;
}
