package org.jmaa.sdk.tenants;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jmaa.sdk.bus.EventBus;
import org.jmaa.sdk.core.Loader;
import org.jmaa.sdk.core.Registry;
import org.jmaa.sdk.data.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 租户信息，租户使用数据库隔离
 *
 * @author Eric Liang
 */
public class Tenant {
    private Logger logger = LoggerFactory.getLogger(Tenant.class);
    private String key;
    private String name;
    private Map<String, Object> data = new HashMap<>();
    private Registry registry;
    private Database database;
    private Properties properties;
    private EventBus eventBus = new EventBus();

    /**
     * 构建租户对象
     *
     * @param key
     * @param name
     * @param properties
     */
    public Tenant(String key, String name, Properties properties) {
        this.key = key;
        this.name = name;
        this.properties = properties;
    }

    /**
     * 租户主键
     *
     * @return
     */
    public String getKey() {
        return key;
    }

    /**
     * 租户名称
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * 关闭
     */
    public void close() {
        try {
            getDatabase().close();
        } catch (Exception exc) {
            logger.error("关闭数据库链接失败");
        }
        eventBus.close();
        if (registry != null) {
            registry.close();
            registry = null;
        }
        data.clear();
    }

    /**
     * 获取注册表
     *
     * @return
     */
    public Registry getRegistry() {
        if (registry == null) {
            synchronized (this) {
                if (registry == null) {
                    Database db = getDatabase();
                    registry = new Registry(this);
                    Loader.getLoader().loadModules(db, registry);
                    logger.info(String.format("租户%s启动成功", name));
                }
            }
        }
        return registry;
    }

    /**
     * 获取事件总线
     *
     * @return
     */
    public EventBus getEvents() {
        return eventBus;
    }

    /**
     * 获取租户配置
     *
     * @return
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * 获取租户数据库对象
     *
     * @return
     */
    public Database getDatabase() {
        if (database == null) {
            database = new Database(properties);
        }
        return database;
    }

    /**
     * 获取租户当前缓存的数据
     *
     * @return
     */
    public Map<String, Object> getData() {
        return data;
    }
}
