package org.jmaa.sdk.core;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.Database;
import org.jmaa.sdk.data.xml.SqlTemplateBuilder;
import org.jmaa.sdk.exceptions.PlatformException;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tools.*;
import org.jmaa.sdk.Manifest;
import org.jmaa.sdk.Records;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.util.ClassUtils;

/**
 * 加载器
 *
 * @author Eric Liang
 */
public class Loader extends ClassLoader {
    static Loader instance;
    private static Logger logger = LoggerFactory.getLogger(Loader.class);

    public static void setLoader(Loader loader) {
        instance = loader;
    }

    public static Loader getLoader() {
        if (instance == null) {
            instance = new Loader();
        }
        return instance;
    }

    protected Loader() {
        super(ClassUtils.getDefaultClassLoader());
    }

    public static <T> T get(Class<T> clz) {
        return getLoader().newInstance(clz);
    }

    protected <T> T newInstance(Class<T> clz) {
        try {
            return (T) getLoader().loadClass(clz.getName()).newInstance();
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    public static void loadModules(Database db, Registry registry) {
        ModelBuilder builder = get(ModelBuilder.class);
        getLoader().loadModules(builder, db, registry);
    }

    void updateLock(Database db) {
        try (Cursor cr = db.openCursor()) {
            boolean exists = cr.getSqlDialect().tableExists(cr, "ir_lock");
            if (!exists) {
                cr.execute("CREATE TABLE `ir_lock` (`id` varchar(13) NOT NULL, `name` varchar(240) DEFAULT NULL COMMENT '名称', `lock_time` timestamp NULL DEFAULT NULL COMMENT '锁定时间', `release_time` timestamp NULL DEFAULT NULL COMMENT '释放时间', `locked` bit(1) DEFAULT NULL COMMENT '是否锁定', `create_uid` varchar(13) DEFAULT NULL COMMENT '创建人', `create_date` timestamp NULL DEFAULT NULL COMMENT '创建时间', `update_uid` varchar(13) DEFAULT NULL COMMENT '修改人',`update_date` timestamp NULL DEFAULT NULL COMMENT '修改时间', PRIMARY KEY (`id`)) COMMENT='ir.lock'");
            }
            String sql = "select locked,release_time from ir_lock where name=%s";
            cr.execute(sql, Arrays.asList("module"));
            if (cr.getRowCount() > 0) {
                sql = "update ir_lock set locked=%s,lock_time=%s,release_time=%s,update_uid=%s,update_date=%s where name=%s";
                Timestamp now = new Timestamp(System.currentTimeMillis());
                Timestamp releaseTime = new Timestamp(DateUtils.addMinutes(now, 5).getTime());
                cr.execute(sql, Arrays.asList(true, now, releaseTime, Constants.SYSTEM_USER, now, "module"));
            } else {
                sql = "insert into ir_lock(id,name,lock_time,release_time,locked,create_uid,create_date,update_uid,update_date)"
                        + "values (%s,%s,%s,%s,%s,%s,%s,%s,%s)";
                Timestamp now = new Timestamp(System.currentTimeMillis());
                Timestamp releaseTime = new Timestamp(DateUtils.addMinutes(now, 5).getTime());
                cr.execute(sql, Arrays.asList(IdWorker.nextId(), "module", now, releaseTime,
                        true, Constants.SYSTEM_USER, now, Constants.SYSTEM_USER, now));
            }
            cr.commit();
        } catch (Exception e) {
            throw new PlatformException(e);
        }
    }

    void forceUpdate(Cursor cr) {
        String forceUpdate = (String) SpringUtils.getProperty("forceUpdate");
        if (StringUtils.isNotEmpty(forceUpdate)) {
            if ("all".equals(forceUpdate)) {
                cr.execute("UPDATE ir_module SET state='installing' WHERE state='installed'");
            } else {
                List<String> modules = Arrays.asList(forceUpdate.split(","));
                cr.execute("UPDATE ir_module SET state='installing' WHERE state='installed' AND name in %s", Collections.singletonList(modules));
            }
        }
    }

    public enum LogLevel {
        Info,
        Error
    }

    public static void addModuleLog(Database db, String name, String content, LogLevel level) {
        try (Cursor cr = db.openCursor()) {
            String sql = "insert into ir_module_log(id,name,content,log_time,state,level) values (%s,%s,%s,%s,%s,%s)";
            Timestamp now = new Timestamp(System.currentTimeMillis());
            cr.execute(sql, Arrays.asList(IdWorker.nextId(), name, content, now, true, level.toString().toLowerCase()));
            cr.commit();
        } catch (Exception exc) {
            logger.error("添加升级日志失败", exc);
        }
    }

    void clearModuleLog(Database db) {
        try (Cursor cr = db.openCursor()) {
            boolean exists = cr.getSqlDialect().tableExists(cr, "ir_module_log");
            if (!exists) {
                cr.execute("CREATE TABLE `ir_module_log` (`id` varchar(13) NOT NULL, `name` varchar(240) DEFAULT NULL COMMENT '名称', `content` mediumtext COMMENT '内容', `level` varchar(240) DEFAULT NULL, `log_time` timestamp NULL DEFAULT NULL COMMENT '时间', `state` bit(1) DEFAULT NULL COMMENT '状态', PRIMARY KEY (`id`)) COMMENT='ir.module.log'");
            }
            String sql = "update ir_module_log set state=%s where state=%s;";
            cr.execute(sql, Arrays.asList(false, true));
            cr.commit();
        } catch (Exception exc) {
            logger.error("clearModuleLog失败", exc);
        }
    }

    void logNewModule(Database db, List<Map<String, Object>> newModules) {
        Function<Map<String, Object>, String> getName = (m) -> {
            Manifest manifest = (Manifest) m.get("manifest");
            return manifest.label() + "(" + manifest.name() + ")";
        };
        if (newModules.size() > 0) {
            addModuleLog(db, "升级/安装模块", newModules.stream().map(getName).collect(Collectors.joining("\n")), LogLevel.Info);
        }
    }

    @SuppressWarnings("unchecked")
    protected void loadModules(ModelBuilder builder, Database db, Registry registry) {
        builder.buildBaseModel(registry);
        try (Cursor cr = db.openCursor()) {
            boolean init = cr.getSqlDialect().tableExists(cr, "ir_module");
            if (!init) {
                init(builder, cr, registry.getTenant());
            }
            addModuleLog(db, "load modules", HttpUtils.getIpAddress().stream().collect(Collectors.joining(",")), LogLevel.Info);
            clearModuleLog(db);
            // 同步加锁
            updateLock(db);
            // 强制更新
            forceUpdate(cr);
            // load installed and installing modules
            List<Map<String, Object>> newModules = loadModuleGraph(builder, cr, registry);
            logNewModule(db, newModules);
            // 设置模型
            registry.setupModels(cr);
            Environment env = new Environment(registry, cr, Constants.SYSTEM_USER);
            // 初始化自定义模型
            Collection<String> customModels = registry.getModels().values().stream().filter(m -> m.isCustom()).map(m -> m.getName()).collect(Collectors.toList());
            cr.commit();
            registry.initModels(env, customModels, "base", false);
            // 初始化模型并建表
            for (Map<String, Object> m : newModules) {
                Collection<String> models = (Collection<String>) m.get("models");
                registry.initModels(env, models, (String) m.get("name"), false);
            }
            Records irModule = env.get("ir.module");
            registry.getModules().forEach((name, module) -> {
                irModule.call("registerBeans", module.getManifest());
                SqlTemplateBuilder.getBuilder().loadTemplates(registry, module.getPackageName(), module.getManifest().scripts());
            });
            // 加载模型数据
            for (Map<String, Object> m : newModules) {
                String pkg = (String) m.get("package_info");
                env.get("ir.model.data").call("loadData", pkg);
                env.get("res.lang").call("loadLang", pkg);
                executeSql(cr, pkg, "data/up.sql");
                Manifest manifest = (Manifest) m.get("manifest");
                String postInstall = manifest.postInstall();
                if (StringUtils.isNotEmpty(postInstall)) {
                    String[] parts = postInstall.split("::");
                    if (parts.length > 1) {
                        env.get(parts[0]).call(parts[1]);
                    }
                }
                cr.commit();
            }
            try (Cursor postInitCursor = db.openCursor()) {
                postInitCursor.setAutoCommit(true);
                Environment envPostInit = new Environment(registry, postInitCursor, Constants.SYSTEM_USER);
                registry.postInit(envPostInit);
            } catch (Exception exc) {
                logger.error("执行postInit失败", exc);
                addModuleLog(db, "执行postInit失败", ThrowableUtils.getDebug(exc), LogLevel.Error);
            }
            if (newModules.size() > 0 && ObjectUtils.toBoolean(SpringUtils.getProperty("updatePermission"), true)) {
                env.get("rbac.permission").call("refresh");
            }
            cr.execute("UPDATE ir_module SET state='installed' WHERE state='installing'");
            // remove modules
            cr.execute("SELECT id,name,package_info FROM ir_module WHERE state ='removing'");
            List<Map<String, Object>> rows = cr.fetchMapAll();
            if (rows.size() > 0) {
                rows.stream().map(m -> (String) m.get("package_info")).forEach(pkg -> {
                    Manifest manifest = ManifestUtils.getManifest(pkg);
                    irModule.call("unregisterBeans", manifest);
                    executeSql(cr, pkg, "data/down.sql");
                });
                cr.execute("UPDATE ir_module SET state='installable' WHERE state='removing'");
            }
            env.get("rbac.permission").call("initModelAuthFields");
            env.get("base").flush();
            for (MetaModule module : registry.getModules().values()) {
                String postInit = module.getManifest().postInit();
                if (StringUtils.isNotEmpty(postInit)) {
                    String[] parts = postInit.split("::");
                    if (parts.length > 1) {
                        env.get(parts[0]).call(parts[1]);
                    }
                }
            }
            env.get("ir.lock").call("unlock", "module");
            cr.commit();
        } catch (Exception exc) {
            logger.error("加载模块失败", exc);
            try (Cursor unlock = db.openCursor()) {
                String sql = "update ir_lock set locked=%s,release_time=%s,update_uid=%s,update_date=%s where name=%s";
                Timestamp now = new Timestamp(System.currentTimeMillis());
                unlock.execute(sql, Arrays.asList(false, now, Constants.SYSTEM_USER, now, "module"));
                unlock.commit();
            } catch (Exception e) {
                throw new PlatformException(e);
            }
            addModuleLog(db, "加载模块启动失败", ThrowableUtils.getDebug(exc), LogLevel.Error);
        }
        registry.loaded = true;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> loadModuleGraph(ModelBuilder builder, Cursor cr, Registry registry) {
        Map<String, Manifest> manifestMap = ManifestUtils.scanModules("**/modules");
        Map<String, Map<String, Object>> nameModules = new HashMap<>(manifestMap.size());
        for (Entry<String, Manifest> entry : manifestMap.entrySet()) {
            nameModules.put(entry.getValue().name(), new HashMap<String, Object>() {{
                put("package_info", entry.getKey());
                put("manifest", entry.getValue());
                put("name", entry.getValue().name());
            }});
        }
        cr.execute("SELECT id,name,package_info,state FROM ir_module WHERE state in ('installed', 'installing')");
        List<Map<String, Object>> rows = cr.fetchMapAll();
        System.out.print("\r加载模块：0/" + rows.size());
        List<String> modules = new ArrayList<>(rows.size());
        for (Map<String, Object> data : rows) {
            String name = (String) data.get("name");
            Map<String, Object> map = nameModules.get(name);
            if (map != null) {
                map.put("state", data.get("state"));
            }
            modules.add(name);
        }
        List<Map<String, Object>> newModules = new ArrayList<>();
        int counter = 0;
        newModules.addAll(buildModule(builder, cr, registry, "base", nameModules));
        for (String module : modules) {
            newModules.addAll(buildModule(builder, cr, registry, module, nameModules));
            System.out.print("\r加载模块：" + (++counter) + "/" + rows.size());
        }
        System.out.println();
        return newModules;
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> buildModule(ModelBuilder builder, Cursor cr, Registry registry, String module,
                                          Map<String, Map<String, Object>> nameModules) {
        List<Map<String, Object>> newModules = new ArrayList<>();
        if (!registry.getModules().containsKey(module)) {
            Map<String, Object> data = nameModules.get(module);
            if (data == null) {
                logger.error(String.format("模块%s不存在", module));
            } else {
                Manifest manifest = (Manifest) data.get("manifest");
                for (String dep : manifest.depends()) {
                    if (StringUtils.isNotEmpty(dep)) {
                        newModules.addAll(buildModule(builder, cr, registry, dep, nameModules));
                    }
                }
                String state = (String) data.get("state");
                if (ObjectUtils.isEmpty(state)) {
                    cr.execute("UPDATE ir_module SET state='installing' WHERE name=%s", Collections.singletonList(module));
                    if (cr.getRowCount() == 0) {
                        cr.execute("INSERT INTO ir_module(id,name,package_info,label,author,state,latest_version,description,application,license) values (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
                                Arrays.asList(IdWorker.nextId(), module, data.get("package_info"), manifest.label(), manifest.author(), "installing", manifest.version(), manifest.description(), manifest.application(), manifest.license()));
                    }
                    state = "installing";
                }
                if ("installing".equals(state)) {
                    newModules.add(data);
                }
                data.put("models", builder.buildModule(registry, manifest));
                registry.getModules().put(module, new MetaModule((String) data.get("package_info"), manifest));
            }
        }
        return newModules;
    }

    void init(ModelBuilder builder, Cursor cr, Tenant tenant) {
        Registry registry = new Registry(tenant);
        builder.buildBaseModel(registry);
        Manifest manifest = ManifestUtils.getManifest(Constants.BASE_PACKAGE);
        builder.buildModule(registry, manifest);
        registry.setupModels(cr);
        Environment env = new Environment(registry, cr, "__system__");
        registry.initModels(env, registry.getModels().keySet(), "base", true);
        registry.postInit(env);
        env.get("ir.model.data").call("loadData", Constants.BASE_PACKAGE);
        env.get("rbac.permission").call("refresh");
        env.get("base").flush();
    }

    void executeSql(Cursor cr, String pkg, String file) {
        String path = PathUtils.combine(pkg.replaceAll("\\.", "/"), file);
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        if (loader != null) {
            InputStream input = loader.getResourceAsStream(path);
            if (input != null) {
                try (InputStreamReader reader = new InputStreamReader(input);
                     BufferedReader bufferedReader = new BufferedReader(reader)) {
                    List<String> lines = new ArrayList<>();
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        lines.add(line);
                    }
                    String strings = lines.stream().collect(Collectors.joining("\r\n"));
                    String[] sqlArray = strings.split(";");
                    for (String sql : sqlArray) {
                        if (StringUtils.isNotEmpty(sql)) {
                            cr.execute(sql);
                        }
                    }
                } catch (Exception ex) {
                    logger.warn(path + ":" + ex.getMessage(), ex);
                }
            }
        }
    }
}
