package org.jmaa.sdk.tools;

import org.jmaa.sdk.Manifest;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.exceptions.ValueException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Eric Liang
 */
public class ManifestUtils {

    static Package getPackage(String module) {
        try {
            Class<?> clazz = Class.forName(module + ".package-info");
            return clazz.getPackage();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Manifest getManifest(String packageName) {
        Package pkg = getPackage(packageName);
        if (pkg == null) {
            throw new ValueException("加载模块:[" + packageName + "]失败，请检查package-info");
        }
        Manifest manifest = pkg.getAnnotation(Manifest.class);
        if (manifest == null) {
            throw new ValueException("包[" + packageName + "]未定义Manifest");
        }
        return manifest;
    }

    static final String CLASSPATH_ALL_URL_PREFIX = "classpath*:";
    static final String PACKAGE_INFO_PATTERN = "**/package-info.class";

    /**
     * 扫描包中的{@link Manifest}，
     * 一个jar包里可以定义多个{@link Manifest}，
     * 但为了最小粒度部署，建议一个jar包只定义一个{@link Manifest}
     *
     * @param basePackage
     * @return
     */
    public static Map<String, Manifest> scanModules(String basePackage) {
        Map<String, Manifest> result = new HashMap<>(16);
        Package pkg = getPackage(Constants.BASE_PACKAGE);
        Manifest base = pkg.getAnnotation(Manifest.class);
        result.put(Constants.BASE_PACKAGE, base);
        try {
            String packageSearchPath = CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(basePackage) + '/' + PACKAGE_INFO_PATTERN;
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(packageSearchPath);
            SimpleMetadataReaderFactory readerFactory = new SimpleMetadataReaderFactory();
            for (Resource resource : resources) {
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                AnnotationMetadata am = reader.getAnnotationMetadata();
                if (am.isAnnotated(Manifest.class.getName())) {
                    ClassLoader loader = resolver.getClassLoader();
                    if (loader != null) {
                        Class<?> packageInfo = loader.loadClass(am.getClassName());
                        Manifest manifest = packageInfo.getAnnotation(Manifest.class);
                        result.put(packageInfo.getPackage().getName(), manifest);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
