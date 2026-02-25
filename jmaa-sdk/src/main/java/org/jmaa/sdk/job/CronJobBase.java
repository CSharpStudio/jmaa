package org.jmaa.sdk.job;

import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.tools.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CronJobBase {

    private final static Logger logger = LoggerFactory.getLogger(CronJobBase.class);

    public Logger getLogger() {
        return logger;
    }

    public Environment getEnv() {
        Environment result = Environment.envs.get();
        if (result == null) {
            throw new ValueException("env未初始化");
        }
        return result;
    }

    public void AddLog(String content) {
        Map<String, Object> ctx = getEnv().getContext();
        List<String> list = (List<String>) ctx.get("$logContent");
        if (list == null) {
            list = new ArrayList<>();
            ctx.put("$logContent", list);
        }
        list.add("->" + content);
    }

    public Map getParams(String params) {
        Map args = ObjectUtils.fromJsonString(params);
        return (Map) args.get("params");
    }
}
