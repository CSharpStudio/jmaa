package jmaa.modules.cron.job.models;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import jmaa.modules.cron.job.utils.JobInterceptor;
import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.job.CronJob;
import net.sf.cglib.proxy.Enhancer;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;

@Model.Meta(inherit = "ir.module")
public class IrModule extends Model {
    public void uninstallModule(Records module) {
        Records tasks = module.getEnv().get("job.task").find(Criteria.equal("module_id", module.getId()));
        tasks.call("onDeleted");
        callSuper(module);
    }

    public void registerBeans(Records rec, Manifest manifest) {
        callSuper(rec, manifest);
        for (Class<?> clazz : manifest.jobs()) {
            initJobHandler(clazz);
        }
    }

    /**
     * 注册JobHandler
     */
    private void initJobHandler(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        Enhancer e = new Enhancer();
        e.setSuperclass(clazz);
        e.setCallback(JobInterceptor.INTERCEPTOR);
        Object bean = e.create();
        for (Method method : methods) {
            CronJob cronJob = AnnotationUtils.findAnnotation(method, CronJob.class);
            if (cronJob != null) {
                String name = cronJob.value();
                if (Utils.isBlank(name)) {
                    throw new PlatformException("CronJob参数不正确：[" + clazz.getName() + "#" + method.getName() + "]");
                }
                if (method.getParameterTypes().length != 1 || !method.getParameterTypes()[0].isAssignableFrom(String.class)) {
                    throw new PlatformException("CronJob方法参数类型不正确：[" + clazz.getName() + "#" + method.getName() + "]，正确示例：public Object execute(String param)");
                }
                if (!method.getReturnType().isAssignableFrom(ReturnT.class) && !Object.class.equals(method.getReturnType())) {
                    throw new PlatformException("CronJob方法返回类型不正确：[" + clazz.getName() + "#" + method.getName() + "]，正确示例：public Object execute(String param)");
                }
                method.setAccessible(true);
                IJobHandler iJobHandler = XxlJobExecutor.loadJobHandler(name);
                if (Utils.isEmpty(iJobHandler)) {
                    XxlJobExecutor.registJobHandler(name, new MethodJobHandler(bean, method, null, null));
                }
            }
        }
    }
}
