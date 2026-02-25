package org.jmaa.sdk.core;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.jmaa.sdk.Records;
import org.jmaa.sdk.exceptions.ModelException;
import net.sf.cglib.proxy.Enhancer;


/**
 * 方法元数据
 *
 * @author Eric Liang
 */
public class MetaMethod {
    private Method method;
    static HashMap<Class<?>, BaseModel> instances = new HashMap<>();

    public MetaMethod(Method method) {
        this.method = method;
        method.setAccessible(true);
    }

    public Method getMethod() {
        return this.method;
    }

    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    public boolean isParameterMatch(Class<?>[] y) {
        Class<?>[] x = method.getParameterTypes();
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < x.length; i++) {
            if (x[i] != y[i]) {
                return false;
            }
        }
        return true;
    }

    public Object invoke(Records rec, Object[] args) {
        BaseModel obj = getInstance();
        try {
            if (method.getParameterCount() == args.length) {
                return method.invoke(obj, args);
            } else {
                return method.invoke(obj, getArgs(rec, args));
            }
        } catch (Exception exc) {
            throw new ModelException(
                String.format("方法[%s.%s]执行失败", method.getDeclaringClass().getName(), method.getName()), exc);
        }
    }

    private Object[] getArgs(Records rec, Object[] args) {
        Object[] result = new Object[args.length + 1];
        result[0] = rec;
        for (int i = 0; i < args.length; i++) {
            result[i + 1] = args[i];
        }
        return result;
    }

    protected BaseModel getInstance() {
        Class<?> clazz = method.getDeclaringClass();
        if (instances.containsKey(clazz)) {
            return instances.get(clazz);
        }
        Enhancer e = new Enhancer();
        e.setSuperclass(clazz);
        e.setCallback(ModelInterceptor.INTERCEPTOR);
        BaseModel instance = (BaseModel) e.create();
        instances.put(clazz, instance);
        return instance;
    }
}
