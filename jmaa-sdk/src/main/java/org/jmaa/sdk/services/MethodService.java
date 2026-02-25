package org.jmaa.sdk.services;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.tools.*;

import org.jmaa.sdk.util.ParamIn;
import org.jmaa.sdk.util.ParamOut;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * 方法服务
 *
 * @author Eric Liang
 */
public class MethodService extends Service {
    private Logger logger = LoggerFactory.getLogger(MethodService.class);
    Method method;

    public MethodService(String name, Model.ServiceMethod svc, Method method) {
        this.method = method;
        setName(name);
        setLabel(svc.label());
        setAuth(svc.auth());
        setDescription(svc.doc());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void execute(ParamIn in, ParamOut out) {
        Records rec = in.getEnv().get(in.getModel());
        Parameter[] params = method.getParameters();
        Map<String, Object> inArgs = new HashMap<>(ObjectUtils.defaultIfNull(in.getArgs(), Collections.emptyMap()));
        Object[] args = new Object[params.length];
        ObjectMapper mapper = new ObjectMapper();
        for (int i = 1; i < params.length; i++) {
            String argName = params[i].getName();
            Object obj = inArgs.remove(argName);
            args[i] = mapper.convertValue(obj, params[i].getType());
        }
        Object idsArg = inArgs.get("ids");
        if (idsArg instanceof List<?>) {
            args[0] = rec.browse((List<String>) idsArg);
        } else {
            args[0] = rec;
        }
        Object obj = rec.call(method.getName(), args);
        out.putData(obj);
    }

    @Override
    public Map<String, ApiDoc> getArgsDoc(MetaModel meta) {
        Parameter[] params = method.getParameters();
        Map<String, ApiDoc> result = new HashMap<String, ApiDoc>(params.length);
        Model.ServiceMethod anno = method.getAnnotation(Model.ServiceMethod.class);
        if (anno.ids()) {
            result.put("ids", new ApiDoc("id集合", Arrays.asList("01ogtcwblyuio")));
        }
        boolean first = true;
        for (Parameter param : params) {
            if (first) {
                first = false;
                if (param.getType() == Records.class) {
                    continue;
                }
            }
            Doc doc = param.getAnnotation(Doc.class);
            Class<?> paramType = param.getType();
            Object example = "";
            String description = "";
            if (doc != null) {
                description = Utils.getOrDefault(doc.doc(), doc.value());
                String value = doc.sample();
                if (StringUtils.isNoneBlank(value)) {
                    ObjectMapper m = new ObjectMapper();
                    try {
                        example = m.readValue(value, paramType);
                    } catch (Exception e) {
                        logger.warn("方法[{}.{}]的参数[{}]示例[{}]反序列化失败：{}", method.getDeclaringClass().getName(),
                            method.getName(), param.getName(), value, ThrowableUtils.getDebug(e));
                        example = DocUtils.getExampleValue(paramType);
                    }
                } else {
                    example = DocUtils.getExampleValue(paramType);
                }
            } else {
                example = DocUtils.getExampleValue(paramType);
            }
            result.put(param.getName(), new ApiDoc(description, example, DocUtils.getJsonType(paramType)));
        }
        return result;
    }

    @Override
    public ApiDoc getResultDoc(MetaModel meta) {
        Doc doc = method.getAnnotation(Doc.class);
        Model.ServiceMethod anno = method.getAnnotation(Model.ServiceMethod.class);
        String desc = doc != null ? Utils.getOrDefault(doc.doc(), doc.value()) : "";
        Class<?> methodType = method.getReturnType();
        Object example = "";
        if (methodType != Void.class) {
            String value = doc != null ? doc.sample() : "";
            if (StringUtils.isNotEmpty(value)) {
                ObjectMapper m = new ObjectMapper();
                try {
                    example = m.readValue(value, methodType);
                } catch (Exception e) {
                    logger.warn("方法[{}.{}]的返回示例[{}]反序列化失败：{}", method.getDeclaringClass().getName(), method.getName(),
                        value, ThrowableUtils.getDebug(e));
                    example = DocUtils.getExampleValue(methodType);
                }
            }
        }
        return new ApiDoc(desc, example, DocUtils.getJsonType(methodType));
    }
}
