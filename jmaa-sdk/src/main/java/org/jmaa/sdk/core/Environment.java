package org.jmaa.sdk.core;

import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.function.Supplier;

import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Database;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.data.xml.SqlTemplateBuilder;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tools.ArrayUtils;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.Cache;
import org.jmaa.sdk.util.ToUpdate;
import org.jmaa.sdk.bus.Subscriber;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.MissingException;
import org.apache.commons.collections4.SetUtils;

import org.jmaa.sdk.Records;

/**
 * 环境上下文
 *
 * @author Eric Liang
 */
public class Environment {
    public static ThreadLocal<Environment> envs = new ThreadLocal<>();
    Registry registry;
    Map<String, Object> context;
    Cursor cursor;
    String uid;
    Boolean isAdmin;
    Records user;
    Records company;
    Records companies;

    static ThreadLocal<Cache> cache = ThreadLocal.withInitial(() -> new Cache());
    static ThreadLocal<ToUpdate> toUpdate = ThreadLocal.withInitial(() -> new ToUpdate());

    /**
     * 关闭
     */
    public void close() {
        cursor.close();
        cache.remove();
        toUpdate.remove();
    }

    /**
     * 创建实例
     *
     * @param registry
     * @param cursor
     * @param uid
     * @param context
     */
    public Environment(Registry registry, Cursor cursor, String uid, Map<String, Object> context) {
        this.registry = registry;
        this.cursor = cursor;
        this.uid = uid;
        if (context == null) {
            context = new HashMap<>();
        }
        this.context = context;
    }

    public Environment(Registry registry, Cursor cursor, String uid) {
        this(registry, cursor, uid, new HashMap<>());
    }

    /**
     * 获取上下文
     *
     * @return
     */
    public Map<String, Object> getContext() {
        return context;
    }

    /**
     * 获取缓存
     *
     * @return
     */
    public Cache getCache() {
        return cache.get();
    }

    /**
     * 获取待更新数据
     *
     * @return
     */
    public ToUpdate getToUpdate() {
        return toUpdate.get();
    }

    /**
     * 获取注册表
     *
     * @return
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * 获取数据访问游标
     *
     * @return
     */
    public Cursor getCursor() {
        return cursor;
    }

    /**
     * 获取数据库管理对象
     *
     * @return
     */
    public Database getDatabase() {
        return registry.getTenant().getDatabase();
    }

    /**
     * 获取当前用户id
     *
     * @return
     */
    public String getUserId() {
        return uid;
    }

    /**
     * 判断当前用户是否管理员
     *
     * @return
     */
    public boolean isAdmin() {
        if (isAdmin == null) {
            isAdmin = (Boolean) get("rbac.security").call("isAdmin", uid);
        }
        return isAdmin;
    }

    /**
     * 获取当前用户数据集
     *
     * @return
     */
    public Records getUser() {
        if (user == null) {
            user = get("rbac.user", uid);
        }
        return user;
    }

    /**
     * 用户当前的公司。
     * 如果没指定company_ids上下文，默认使用用户的主公司。
     *
     * @return 当前公司，默认是用户的主公司
     * @throws AccessException 上下文中company_ids的公司无效或者没有权限
     */
    @SuppressWarnings("unchecked")
    public Records getCompany() {
        if (company == null) {
            List<String> companyIds = getCompanyIds();
            if (companyIds.size() > 0) {
                Set<String> userCompanyIds = SetUtils.hashSet(getUser().getRec("company_ids").getIds());
                if (!userCompanyIds.contains(companyIds.get(0))) {
                    throw new AccessException("访问未授权或无效的公司");
                }
                company = get("res.company", companyIds.get(0));
            } else {
                company = getUser().getRec("company_id");
            }
        }
        return company;
    }

    List<String> getCompanyIds() {
        String[] array = ((String) context.getOrDefault("company_ids", "")).split(",");
        List<String> companyIds = new ArrayList<>();
        for (String item : array) {
            if (StringUtils.isNotEmpty(item)) {
                companyIds.add(item);
            }
        }
        return companyIds;
    }

    /**
     * 用户访问的公司集。
     * 如果没指定company_ids上下文，默认使用用户的所有公司。
     *
     * @return
     * @throws AccessException 上下文中company_ids的公司无效或者没有权限
     */
    public Records getCompanies() {
        if (companies == null) {
            List<String> companyIds = getCompanyIds();
            if (companyIds.size() > 0) {
                Set<String> userCompanyIds = SetUtils.hashSet(getUser().getRec("company_ids").getIds());
                for (String cid : companyIds) {
                    if (!userCompanyIds.contains(cid)) {
                        throw new AccessException("访问未授权或无效的公司");
                    }
                }
                companies = get("res.company", companyIds);
            } else {
                companies = getUser().getRec("company_ids");
            }
        }
        return companies;
    }

    /**
     * 查找xml id关联的数据集
     *
     * @param xmlId
     * @return
     */
    public Records findRef(String xmlId) {
        return (Records) get("ir.model.data").call("findRef", xmlId);
    }

    /**
     * 获取xml id关联的数据集
     *
     * @param xmlId
     * @return
     * @throws MissingException 找不到xml id的数据集
     */
    public Records getRef(String xmlId) {
        Records result = findRef(xmlId);
        if (result == null) {
            throw new MissingException(l10n("找不到xml id：%s", xmlId));
        }
        return result;
    }

    /**
     * 获取定义在xml中的sql
     *
     * @param xmlId
     * @return
     */
    public SqlTemplate getSql(String xmlId) {
        Map<String, FutureTask<SqlTemplate>> templates = registry.getTemplates();
        String dialect = getCursor().getSqlDialect().getName();
        FutureTask<SqlTemplate> task = templates.get(xmlId + "Ø" + dialect);
        if (task == null) {
            task = templates.get(xmlId);
        }
        if (task == null) {
            throw new MissingException("SQL模板id不存在:" + xmlId);
        }
        try {
            return task.get();
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    /**
     * 获取模型的空数据集
     *
     * @param model
     * @return
     */
    public Records get(String model) {
        MetaModel meta = registry.get(model);
        return meta.browse(this, ArrayUtils.EMPTY_STRING_ARRAY, null);
    }

    /**
     * 获取模型指定id的数据集
     *
     * @param model
     * @param ids
     * @return
     */
    public Records get(String model, String... ids) {
        MetaModel meta = registry.get(model);
        return meta.browse(this, ids, null);
    }

    /**
     * 获取配置参数模型
     *
     * @return
     */
    public Records getConfig() {
        return get("res.config", "res-config");
    }

    /**
     * 获取模型指定id的数据集
     *
     * @param model
     * @param ids
     * @return
     */
    public Records get(String model, Collection<String> ids) {
        MetaModel meta = registry.get(model);
        return meta.browse(this, ids.toArray(ArrayUtils.EMPTY_STRING_ARRAY), null);
    }

    /**
     * 获取租户数据
     *
     * @param key
     * @return
     */
    public Object getTenantData(String key) {
        Tenant tenant = registry.getTenant();
        if (tenant != null) {
            return tenant.getData().get(key);
        }
        return null;
    }

    /**
     * 获取租户数据，不存在时插入初始值
     *
     * @param <T>
     * @param key
     * @param supplier
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getTenantData(String key, Supplier<T> supplier) {
        Tenant tenant = registry.getTenant();
        if (tenant != null) {
            Map<String, Object> data = tenant.getData();
            if (!data.containsKey(key)) {
                T value = supplier.get();
                // TODO 可能有并发问题
                data.put(key, value);
                return value;
            }
            return (T) data.get(key);
        }
        return supplier.get();
    }

    /**
     * 设置租户数据
     *
     * @param key
     * @param val
     */
    public void setTenantData(String key, Object val) {
        Tenant tenant = registry.getTenant();
        if (tenant != null) {
            tenant.getData().put(key, val);
        }
    }

    /**
     * 获取当前语言代码
     *
     * @return
     */
    public String getLang() {
        String lang = (String) context.get("lang");
        if (StringUtils.isEmpty(lang)) {
            lang = (String) getUser().get("lang");
        }
        if (StringUtils.isEmpty(lang)) {
            lang = "zh_CN";
        }
        return lang;
    }

    /**
     * 获取时区
     *
     * @return
     */
    public String getTimezone() {
        String tz = (String) context.get("tz");
        if (StringUtils.isEmpty(tz)) {
            tz = (String) getUser().get("tz");
        }
        if (StringUtils.isEmpty(tz)) {
            tz = "UTC";
        }
        return tz;
    }

    /**
     * 是否启用调试模式
     * 调试模式下加载用于可调试的资源（例如：未打包的JS源码）
     *
     * @return
     */
    public boolean isDebug() {
        return "true".equals(context.get("debug"));
    }

    /**
     * 本地化翻译
     *
     * @param format
     * @return
     */
    public String l10n(String format, Object... args) {
        if (registry.isLoaded()) {
            String lang = getLang();
            Map<String, String> data = getTenantData("lang@" + lang,
                () -> (Map<String, String>) get("res.lang").call("getLocalization"));
            String value = data.getOrDefault(format, format);
            return String.format(value, args);
        }
        return String.format(format, args);
    }

    /**
     * 订阅消息
     *
     * @param clazz
     * @param subscriber
     * @param <T>
     */
    public <T> void subscribeEvent(Class<T> clazz, Subscriber<T> subscriber) {
        getRegistry().getTenant().getEvents().subscribe(clazz, subscriber);
    }

    /**
     * 发布消息
     *
     * @param event
     */
    public void publishEvent(Object event) {
        getRegistry().getTenant().getEvents().publish(this, event);
    }
}
