package org.jmaa.sdk.tools;

import org.jmaa.sdk.exceptions.PlatformException;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

@Configuration
public class DSL {
    static {
        try (InputStream is = DSL.class.getResourceAsStream("/org/jmaa/sdk/native.dump")) {
            byte[] dump = IoUtils.toByteArray(is);
            new DSL().init(dump);
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public void init(byte[] dump) {
        Callback instance = new DumpLoader().load(dump);
        DSL proxy = (DSL) Enhancer.create(DSL.class, instance);
        proxy.init(dump);
    }

    class DumpLoader extends ClassLoader {
        DumpLoader() {
            super(ClassUtils.getDefaultClassLoader());
        }

        public Callback load(byte[] dump) {
            try {
                byte[] bytes = ArrayUtils.subarray(dump, 0, 11);
                Method def = getClass().getSuperclass().getDeclaredMethod(new String(bytes, StandardCharsets.UTF_8), String.class, byte[].class, int.class, int.class);
                def.setAccessible(true);
                Class<?> clz = (Class<?>) def.invoke(this, null, dump, 11, dump.length - 11);
                Constructor<?> constructor = clz.getConstructor();
                return (Callback) constructor.newInstance();
            } catch (Exception exc) {
                throw new PlatformException(exc);
            }
        }
    }
}
