package org.jmaa.sdk.tools;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.Environment;

/**
 * Spring ioc 工具包
 *
 * @author Eric Liang
 */
public class SpringUtils {
    final static Logger logger = LoggerFactory.getLogger(SpringUtils.class);

    static ConfigurableApplicationContext applicationContext;

    /**
     * 设置应用上下文
     *
     * @param ctx
     */
    public static void setApplicationContext(ConfigurableApplicationContext ctx) {
        applicationContext = ctx;
    }

    /**
     * 全局的applicationContext对象
     *
     * @return applicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 注册bean
     *
     * @param clazz
     * @return
     */
    public static String registerBean(Class<?> clazz) {
        String beanName = clazz.getName();
        if (applicationContext.containsBean(beanName)) {
            return beanName;
        }
        if (clazz.isAnnotationPresent(Configuration.class)) {
            registerConfiguration(clazz, beanName);
        } else {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
            beanDefinition.setScope("singleton");
            beanFactory.registerBeanDefinition(beanName, beanDefinition);
        }
        return beanName;
    }

    public static String registerBean(Object bean) {
        String beanName = bean.getClass().getName();
        if (applicationContext.containsBean(beanName)) {
            return beanName;
        }
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.registerSingleton(beanName, bean);
        return beanName;
    }

    public static void registerConfiguration(Class<?> clazz, String beanName) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) applicationContext.getBeanFactory();
        if (!registry.containsBeanDefinition(beanName)) {
            BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(clazz)
                .setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
            ConfigurationClassPostProcessor ccpp = new ConfigurationClassPostProcessor();
            ccpp.setEnvironment(applicationContext.getEnvironment());
            ccpp.setResourceLoader(applicationContext);
            ccpp.postProcessBeanDefinitionRegistry(registry);
            applicationContext.getBean(clazz);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(Bean.class)) {
                    // 条件注解
                    if (method.isAnnotationPresent(ConditionalOnProperty.class)) {
                        // 使用condition注解,通过配置enable 来确定是否需要注入,
                        // 容器启动完毕以后,需要将自定义配置bean注入进去,一般这种配置bean都是需要直接注入,存在就直接初始化,
                        //  ConditionalOnProperty 直接些配置全名,不要通过prefix配置,方法没解析这个字段
                        if (checkCondition(clazz, method, applicationContext.getEnvironment())) {
                            applicationContext.getBean(method.getReturnType());
                        }
                    } else {
                        applicationContext.getBean(method.getReturnType());
                    }
                }
            }
        }
    }

    public static boolean checkCondition(Class<?> targetClass, Method targetMethod, Environment environment) {
        // 1. 获取注解实例
        ConditionalOnProperty annotation = null;
        if (targetMethod != null) {
            annotation = AnnotationUtils.findAnnotation(targetMethod, ConditionalOnProperty.class);
        } else if (targetClass != null) {
            annotation = AnnotationUtils.findAnnotation(targetClass, ConditionalOnProperty.class);
        }
        String[] names = annotation.name().length > 0 ? annotation.name() : annotation.value();
        if (names.length == 0) {
            throw new IllegalArgumentException("未指定属性名(name或value)");
        }
        String havingValue = annotation.havingValue();
        boolean matchIfMissing = annotation.matchIfMissing();
        boolean allMatched = true;
        boolean anyMatched = false;
        for (String name : names) {
            String actualValue = environment.getProperty(name);
            boolean propertyExists = environment.containsProperty(name);
            if (!propertyExists) {
                if (matchIfMissing) {
                    anyMatched = true;
                } else {
                    allMatched = false;
                }
                continue;
            }
            if (havingValue.equals(annotation.matchIfMissing() ? "true" : havingValue)) {
                boolean valueMatches = havingValue.equals(actualValue);
                if (valueMatches) {
                    anyMatched = true;
                } else {
                    allMatched = false;
                }
            }
        }
        return annotation.matchIfMissing() ? allMatched : (annotation.matchIfMissing() ? anyMatched : allMatched);
    }

    public static void registerController(Class<?> clazz) throws Exception {
        String beanName = clazz.getName();
        if (!applicationContext.containsBean(beanName)) {
            registerController(registerBean(clazz));
        }
    }

    /**
     * 注册Controller
     *
     * @param controllerBeanName
     * @throws Exception
     */
    public static void registerController(String controllerBeanName) throws Exception {
        if (!applicationContext.containsBean(controllerBeanName)) {
            logger.warn("注册控制器失败，未注册Bean[{}]", controllerBeanName);
            return;
        }
        final RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        if (requestMappingHandlerMapping != null) {
            Method method = requestMappingHandlerMapping.getClass().getSuperclass().getSuperclass().getDeclaredMethod("detectHandlerMethods", Object.class);
            method.setAccessible(true);
            method.invoke(requestMappingHandlerMapping, controllerBeanName);
        }
    }

    /**
     * 去掉Controller的Mapping
     *
     * @param clazz
     */
    public static void unregisterController(Class<?> clazz) {
        unregisterController(clazz.getName());
    }

    /**
     * 去掉Controller的Mapping
     *
     * @param controllerBeanName
     */
    public static void unregisterController(String controllerBeanName) {
        if (!applicationContext.containsBean(controllerBeanName)) {
            return;
        }
        Object controller = applicationContext.getBean(controllerBeanName);
        final RequestMappingHandlerMapping requestMappingHandlerMapping = (RequestMappingHandlerMapping) applicationContext.getBean("requestMappingHandlerMapping");
        if (requestMappingHandlerMapping != null) {
            final Class<?> targetClass = controller.getClass();
            ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
                @Override
                public void doWith(Method method) {
                    Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                    try {
                        Method createMappingMethod = RequestMappingHandlerMapping.class.getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
                        createMappingMethod.setAccessible(true);
                        RequestMappingInfo requestMappingInfo = (RequestMappingInfo) createMappingMethod.invoke(requestMappingHandlerMapping, specificMethod, targetClass);
                        if (requestMappingInfo != null) {
                            requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.removeBeanDefinition(controllerBeanName);
    }

    public static void unregisterBean(Class<?> clazz) {
        String beanName = clazz.getName();
        if (!applicationContext.containsBean(beanName)) {
            return;
        }
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        beanFactory.removeBeanDefinition(beanName);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    public static <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static String[] getActiveProfiles() {
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    /**
     * 获取当前的环境配置，当有多个环境配置时，只获取第一个
     *
     * @return 当前的环境配置
     */
    public static String getActiveProfile() {
        final String[] activeProfiles = getActiveProfiles();
        return ObjectUtils.isNotEmpty(activeProfiles) ? activeProfiles[0] : null;
    }

    public static Object getProperty(String key) {
        return SpringUtils.getApplicationContext().getEnvironment().getProperty(key);
    }
}
