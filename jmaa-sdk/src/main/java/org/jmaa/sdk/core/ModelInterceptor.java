package org.jmaa.sdk.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.tools.Profiler;
import org.jmaa.sdk.Utils;
import org.springframework.core.NamedThreadLocal;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * @author Eric Liang
 */
public class ModelInterceptor implements MethodInterceptor {

    public static final ModelInterceptor INTERCEPTOR = new ModelInterceptor();

    public final static NamedThreadLocal<Boolean> INVOKE_CURRENT = new NamedThreadLocal<>("invoke current method");

    @Override
    public Object intercept(Object target, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (INVOKE_CURRENT.get() != null) {
            INVOKE_CURRENT.remove();
            try (AutoCloseable m = Profiler.monitor(() -> Utils.format("method:%s,args:[%s]", method.getName(), getArgs(args)))) {
                return proxy.invokeSuper(target, args);
            }
        }
        if (args.length > 0 && args[0] instanceof Records) {
            Records rec = (Records) args[0];
            MetaModel meta = rec.getMeta();
            MetaMethod m = meta.findOverrideMethod(method);
            if (m != null) {
                INVOKE_CURRENT.set(true);
                return m.invoke(rec, args);
            }
        }
        return proxy.invokeSuper(target, args);
    }

    String getArgs(Object[] args) {
        List<String> result = new ArrayList<>();
        for (Object arg : args) {
            result.add(Utils.toString(arg));
        }
        return Utils.join(result);
    }
}
