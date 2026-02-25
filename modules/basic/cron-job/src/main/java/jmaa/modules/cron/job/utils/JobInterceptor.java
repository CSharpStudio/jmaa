package jmaa.modules.cron.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.xxl.job.core.biz.model.ReturnT;
import org.jmaa.sdk.job.CronJob;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.ThrowableUtils;
import org.jmaa.sdk.util.KvMap;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobInterceptor implements MethodInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(JobInterceptor.class);

    public static final JobInterceptor INTERCEPTOR = new JobInterceptor();

    @Override
    public Object intercept(Object target, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (!method.isAnnotationPresent(CronJob.class)) {
            return proxy.invokeSuper(target, args);
        }
        Map params = JSONObject.parseObject((String) args[0], Map.class);
        Map ctx = new KvMap().set("company_ids", params.get("company_id"));
        Tenant tenant = TenantService.find(Utils.toString(params.get("tenant")));
        Registry reg = tenant.getRegistry();
        Cursor cr = tenant.getDatabase().openCursor();
        Environment env = new Environment(reg, cr, Utils.toString(params.get("user_id")), ctx);
        Environment.envs.set(env);
        String logId = null;
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("status", "run");
            values.put("start_time", new Date());
            values.put("job_id", params.get("job_id"));
            Records log = env.get("job.log").create(values);
            logId = log.getId();
            env.getCursor().commit();
        } catch (Exception exc) {
            logger.error("任务日志保存失败", exc);
        }
        try {
            Object result = proxy.invokeSuper(target, args);
            try {
                List<String> list = (List<String>) env.getContext().get("$logContent");
                Map<String, Object> values = new HashMap<>();
                if (Utils.isNotEmpty(list)) {
                    values.put("content", list.stream().collect(Collectors.joining("\n")));
                }
                values.put("status", "success");
                values.put("end_time", new Date());
                env.get("job.log", logId).update(values);
            } catch (Exception exc) {
                logger.error("任务日志保存失败", exc);
            }
            env.get("base").flush();
            env.getCursor().commit();
            return new ReturnT<>(Utils.toString(result));
        } catch (Throwable t) {
            try {
                List<String> list = (List<String>) env.getContext().get("$logContent");
                Map<String, Object> values = new HashMap<>();
                if (Utils.isNotEmpty(list)) {
                    values.put("content", list.stream().collect(Collectors.joining("\n")));
                }
                values.put("status", "error");
                values.put("end_time", new Date());
                String error = ThrowableUtils.getDebug(t);
                if (error.length() > 2000) {
                    error = error.substring(0, 2000);
                }
                values.put("error", error);
                env.get("job.log", logId).update(values);
                env.get("base").flush();
                env.getCursor().commit();
            } catch (Exception exc) {
                logger.error("任务日志保存失败", exc);
                logger.error("任务执行失败", t);
            }
            throw t;
        } finally {
            env.close();
            Environment.envs.remove();
        }
    }
}
