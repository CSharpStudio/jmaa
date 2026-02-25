package org.jmaa.sdk.services;

import java.util.Map;
import java.util.stream.Collectors;

import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Service;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.tools.DocUtils;

/**
 * 创建模型服务
 *
 * @author Eric Liang
 */
public class CreateService extends Service {
    @Override
    protected void execute(ParamIn in, ParamOut out) {
        Records rec = in.getEnv().get(in.getModel()).create(in.getArgs());
        out.putData(rec.getId());
    }

    @Override
    public Map<String, ApiDoc> getArgsDoc(MetaModel meta) {
        return meta.getFields().entrySet().stream().filter(e -> !e.getValue().isAuto())
                .collect(Collectors.toMap(e -> e.getKey(),
                        e -> new ApiDoc(getHelp(e.getValue()), DocUtils.getExampleValue(e.getValue(), false))));
    }

    String getHelp(MetaField field) {
        String help = field.getHelp();
        if (help == null) {
            help = "";
        }
        return help;
    }

    @Override
    public ApiDoc getResultDoc(MetaModel meta) {
        return new ApiDoc("新记录id", "01m5kvc8dzhfk");
    }
}
