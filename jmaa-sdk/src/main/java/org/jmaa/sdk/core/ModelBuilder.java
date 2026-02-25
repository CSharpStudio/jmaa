package org.jmaa.sdk.core;

import java.util.Collection;

import org.jmaa.sdk.Manifest;

/**
 * 模型建构
 *
 * @author Eric Liang
 */
@SuppressWarnings("AlibabaAbstractClassShouldStartWithAbstractNaming")
public interface ModelBuilder {


    /**
     * 构建基模型，所有模型都是继承自基模型，基模型提供基础CURD方法
     *
     * @param registry
     * @return
     */
    MetaModel buildBaseModel(Registry registry);

    /**
     * 构建模型
     *
     * @param registry
     * @param cls
     * @param module
     * @return
     */
    MetaModel buildModel(Registry registry, BaseModel cls, String module);

    /**
     * 构建模块
     *
     * @param registry
     * @param manifest
     * @return
     */
    Collection<String> buildModule(Registry registry, Manifest manifest);
}
