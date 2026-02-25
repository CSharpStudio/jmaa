package org.jmaa.sdk.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Service;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.tools.DocUtils;
import org.apache.commons.collections4.SetUtils;

/**
 * 更新模型服务
 *
 * @author Eric Liang
 */
public class UpdateService extends Service {
    @Override
    protected void execute(ParamIn in, ParamOut out) {
        UpdateParam param = in.getArgs(UpdateParam.class);
        if (ObjectUtils.isEmpty(param.ids)) {
            throw new PlatformException("更新没提供ids参数");
        }
        Records rec = in.getEnv().get(in.getModel(), param.ids);
        rec.call("checkFieldAccessRights", "write", param.values.keySet());
        if (rec.size() != param.ids.length) {
            Set<String> ids = SetUtils.hashSet(param.ids);
            ids.removeAll(Arrays.asList(rec.getIds()));
            throw new ValidationException(rec.l10n("记录[%s]不存在，可能已被删除", ids));
        } else {
            rec.update(param.values);
            out.putData(true);
        }
    }


    @Override
    public Map<String, ApiDoc> getArgsDoc(MetaModel meta) {
        Object values = meta.getFields().entrySet().stream().filter(e -> !e.getValue().isAuto())
                .collect(Collectors.toMap(e -> e.getKey(), e -> DocUtils.getExampleValue(e.getValue(), false)));
        return new HashMap<String, ApiDoc>(1) {
            {
                put("ids", new ApiDoc("id集合", Arrays.asList("01m5kvc8dzhfk", "01m5kvc8dzhfl")));
                put("values", new ApiDoc("字段/值", values));
            }
        };
    }

    @Override
    public ApiDoc getResultDoc(MetaModel meta) {
        return new ApiDoc("", true);
    }
}

class UpdateParam {
    public String[] ids;
    public Map<String, Object> values;
}
