package org.jmaa.base.models;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Loader;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tenants.TenantService;
import org.jmaa.sdk.tools.SpringUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.tools.IoUtils;
import org.jmaa.sdk.tools.ManifestUtils;
import org.springframework.util.ClassUtils;

/**
 * 模块, 模块是打包发布的最小单位，一个应用包含一个或多个模块，模块之间会有依赖关系，在安装模块时，自动安装依赖
 *
 * @author Eric Liang
 */
@Model.Meta(name = "ir.module", label = "模块", order = "application desc,category_id,name", present = {"label", "name"}, presentFormat = "{label} ({name})")
public class IrModule extends Model {
    static Field name = Field.Char().label("名称").help("模块的名称").required().unique();
    static Field package_info = Field.Char().label("源码包");
    static Field label = Field.Char().label("标题").required();
    static Field author = Field.Char().label("作者").help("版权所有者");
    static Field state = Field.Selection().label("状态").selection(new Options() {{
        put("uninstallable", "不可安装");
        put("installable", "可安装");
        put("installed", "已安装");
        put("removing", "待卸载");
        put("upgradable", "可更新");
        put("installing", "安装中");
    }}).defaultValue("uninstallable");
    static Field image = Field.Image().label("图标").attachment(false);
    static Field latest_version = Field.Char().label("最新版本");
    static Field description = Field.Char().label("说明").help("详细描述模块的功能");
    static Field application = Field.Boolean().label("是否应用");
    static Field license = Field.Char().label("授权");
    static Field model_ids = Field.One2many("ir.model", "module_id");
    static Field category_id = Field.Many2one("ir.module.category").label("分类");
    static Field dependency_ids = Field.One2many("ir.module.dependency", "module_id");

    @Model.ServiceMethod(label = "更新模块清单", ids = false)
    public Object updateModules(Records rec) {
        Map<String, Manifest> manifestMap = ManifestUtils.scanModules("**/modules");
        Records modules = rec.find(new Criteria(), 0, 0, "");
        Map<String, Records> nameModules = new HashMap<>(modules.size());
        for (Records module : modules) {
            nameModules.put((String) module.get("name"), module);
        }
        List<Map<String, Object>> toCreate = new ArrayList<>();
        Map<Records, KvMap> toUpdate = new HashMap<>(manifestMap.size());
        Map<String, String[]> moduleDepends = new HashMap<>(manifestMap.size());
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("select id,name from ir_module_category");
        Map<String, String> categoryIdMap = cr.fetchAll().stream().collect(Collectors.toMap(r -> (String) r[1], r -> (String) r[0]));
        for (Entry<String, Manifest> entry : manifestMap.entrySet()) {
            Manifest manifest = entry.getValue();
            KvMap data = toMap(rec, entry.getKey(), manifest, categoryIdMap);
            String name = manifest.name();
            moduleDepends.put(name, manifest.depends());
            if (nameModules.containsKey(name)) {
                toUpdate.put(nameModules.get(name), data);
            } else {
                if ("tenant".equals(manifest.name()) && !"root".equals(rec.getEnv().getRegistry().getTenant().getKey())) {
                    continue;
                }
                if (manifest.autoInstall()) {
                    data.put("state", "installed");
                } else {
                    data.put("state", "installable");
                }
                toCreate.add(data);
            }
        }
        List<Map<String, Object>> dependsDataList = new ArrayList<>();
        if (!toCreate.isEmpty()) {
            Records newModules = rec.createBatch(toCreate);
            for (Records module : newModules) {
                dependsDataList.addAll(getDepends(module, moduleDepends));
            }
        }
        List<String> toDelete = toUpdate.keySet().stream().map(r -> r.getId()).collect(Collectors.toList());
        cr.execute("DELETE FROM ir_module_dependency WHERE module_id in %s", Arrays.asList(toDelete));
        for (Entry<Records, KvMap> entry : toUpdate.entrySet()) {
            Records module = entry.getKey();
            module.update(entry.getValue());
            dependsDataList.addAll(getDepends(module, moduleDepends));
        }
        rec.getEnv().get("ir.module.dependency").createBatch(dependsDataList);
        return Action.reload(rec.l10n("更新成功"));
    }

    static KvMap toMap(Records rec, String packageInfo, Manifest manifest, Map<String, String> categoryIdMap) {
        String name = manifest.name();
        KvMap data = new KvMap(16);
        data.put("name", name);
        data.put("package_info", packageInfo);
        data.put("label", manifest.label());
        data.put("description", manifest.description());
        data.put("author", manifest.author());
        data.put("latest_version", manifest.version());
        ClassLoader loader = ClassUtils.getDefaultClassLoader();
        if (loader != null) {
            String icon = manifest.icon();
            if (Utils.isEmpty(icon)) {
                icon = packageInfo.replaceAll("\\.", "/") + "/statics/module.png";
            }
            data.put("image", new KvMap().set("name", "icon.png").set("data", loadIcon(loader, manifest.label(), icon)));
        }
        String category = manifest.category();
        if (Utils.isNotBlank(category)) {
            String categoryId = categoryIdMap.get(category);
            if (Utils.isEmpty(categoryId)) {
                Records cate = rec.getEnv().get("ir.module.category").create(new KvMap(1).set("name", category));
                categoryId = cate.getId();
                categoryIdMap.put(category, categoryId);
            }
            data.put("category_id", categoryId);
        }
        data.put("license", manifest.license());
        data.put("application", manifest.application());
        return data;
    }

    static byte[] loadIcon(ClassLoader loader, String label, String icon) {
        byte[] result = null;
        try {
            InputStream input = loader.getResourceAsStream(icon);
            if (input == null) {
                icon = "org/jmaa/base/statics/icons/modules/" + label + ".png";
                input = loader.getResourceAsStream(icon);
            }
            if (input == null) {
                icon = "org/jmaa/base/statics/base.png";
                input = loader.getResourceAsStream(icon);
            }
            result = IoUtils.toByteArray(input);
            input.close();
        } catch (Exception e) {

        }
        return result;
    }

    static List<KvMap> getDepends(Records module, Map<String, String[]> moduleDepends) {
        List<KvMap> result = new ArrayList<>();
        String name = (String) module.get("name");
        String[] depends = moduleDepends.get(name);
        if (depends != null) {
            for (String depend : depends) {
                KvMap d = new KvMap(2);
                d.put("name", depend);
                d.put("module_id", module.getId());
                result.add(d);
            }
        }
        return result;
    }

    public void installModule(Records module) {
        module.set("state", "installing");
    }

    public void uninstallModule(Records module) {
        module.set("state", "removing");
    }

    private List<String> installDepends(Records addon) {
        List<String> result = new ArrayList<>();
        Records deps = (Records) addon.get("dependency_ids");
        List<String> depends = deps.stream().map(d -> (String) d.get("name")).collect(Collectors.toList());
        Records modules = addon.getEnv().get("ir.module").find(Criteria.in("name", depends), 0, -1, null);
        for (Records module : modules) {
            if ("installable".equals(module.getString("state"))) {
                installModule(module);
                result.add(module.getString("name"));
            }
            result.addAll(installDepends(module));
        }
        return result;
    }

    /**
     * 模型安装，在安装模块时，自动安装依赖
     */
    @Model.ServiceMethod(label = "安装应用", doc = "安装应用，并安装相关依赖")
    public Object install(Records rec) {
        checkLock(rec);
        List<String> modules = new ArrayList<>();
        for (Records module : rec) {
            if ("installable".equals(module.getString("state"))) {
                installModule(module);
                modules.add(module.getString("name"));
            }
            modules.addAll(installDepends(module));
        }
        rec.flush();
        String content = String.format("操作人：%s;模块:%s", rec.getEnv().getUser().get("present"), Utils.join(modules));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "安装应用", content, Loader.LogLevel.Info);
        rec.getEnv().getCursor().commit();
        /** 重置租户 */
        resetTenant(rec);
        return Action.js("view.showLog()");
    }

    static void checkLock(Records rec) {
        Map<String, Object> lockInfo = (Map<String, Object>) rec.getEnv().get("ir.lock").call("getLockInfo", "module");
        if (Utils.toBoolean(lockInfo.get("isLocked"))) {
            SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            throw new ValidationException(rec.l10n("当前正在执行升级，开始时间:%s，执行人:%s", sf.format((Date) lockInfo.get("lockTime")), lockInfo.get("lockBy")));
        }
    }

    /**
     * 一键安装所有应用
     */
    @Model.ServiceMethod(label = "安装所有应用", doc = "安装所有未安装应用，并安装相关依赖", ids = false)
    public Object installAll(Records rec) {
        checkLock(rec);
        //先更新依赖再升级
        updateModules(rec);
        Cursor cr = rec.getEnv().getCursor();
        for (Records module : rec.find(Criteria.equal("state", "installable"))) {
            installModule(module);
        }
        String content = String.format("操作人：%s", rec.getEnv().getUser().get("present"));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "安装所有应用", content, Loader.LogLevel.Info);
        rec.flush();
        cr.commit();
        /** 重置租户 */
        resetTenant(rec);
        return Action.js("view.showLog()");
    }

    /**
     * 重启租户，重启后将重新构建模型和加载数据
     */
    @Model.ServiceMethod(label = "重启租户", ids = false)
    public Object resetTenant(Records rec) {
        Tenant tenant = rec.getEnv().getRegistry().getTenant();
        Tenant newTenant = new Tenant(tenant.getKey(), tenant.getName(), tenant.getProperties());
        try {
            newTenant.getRegistry();
            TenantService.register(newTenant);
        } catch (Exception exc) {
            newTenant.close();
            throw exc;
        }
        //关闭释放原来的租户
        tenant.close();
        return Action.js("top.window.location.reload()");
    }

    /**
     * 更新{@link Manifest#data()}中指定的xml文件声明的数据
     *
     * @param rec
     * @return
     */
    @Model.ServiceMethod(label = "更新应用数据")
    public Object updateData(Records rec) {
        String pkg = (String) rec.get("package_info");
        rec.getEnv().get("ir.model.data").call("loadData", pkg);
        String content = String.format("操作人：%s;模块:%s", rec.getEnv().getUser().get("present"), rec.get("name"));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "更新应用数据", content, Loader.LogLevel.Info);
        return Action.reload(rec.l10n("更新成功"));
    }

    @Model.ServiceMethod(label = "卸载应用", doc = "卸载应用，并卸载相关依赖")
    public Object uninstall(Records rec) {
        checkLock(rec);
        List<String> modules = uninstallModules(rec);
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT name FROM ir_module WHERE state='removing'");
        List<String> removing = cr.fetchAll().stream().map(row -> (String) row[0]).collect(Collectors.toList());
        rec.getEnv().get("ir.model.data").call("removeData", removing);
        String content = String.format("操作人：%s;模块:%s", rec.getEnv().getUser().get("present"), Utils.join(modules));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "卸载应用", content, Loader.LogLevel.Info);
        rec.flush();
        cr.commit();
        /** 重置租户 */
        return resetTenant(rec);
    }

    @Model.ServiceMethod(auth = "read", label = "获取依赖指定应用的清单", doc = "获取依赖指定应用的清单")
    public List<Map<String, Object>> getDependOn(Records rec) {
        List<String> names = findDependOn(rec);
        return rec.find(Criteria.in("name", names)).read(Arrays.asList("name", "label", "author"));
    }

    static List<String> findDependOn(Records rec) {
        List<String> result = new ArrayList<>();
        for (Records module : rec) {
            if ("installed".equals(module.getString("state"))) {
                result.add(module.getString("name"));
                Records dependBy = module.find(Criteria.equal("dependency_ids.name", module.get("name")).and(Criteria.equal("state", "installed")));
                result.addAll(findDependOn(dependBy));
            }
        }
        return result;
    }

    public List<String> uninstallModules(Records modules) {
        List<String> result = new ArrayList<>();
        for (Records module : modules) {
            if ("installed".equals(module.getString("state"))) {
                result.add(module.getString("name"));
                uninstallModule(module);
                Records dependBy = module.find(Criteria.equal("dependency_ids.name", module.get("name")).and(Criteria.equal("state", "installed")));
                result.addAll(uninstallModules(dependBy));
            }
        }
        return result;
    }

    /**
     * 升级模块
     */
    @Model.ServiceMethod(label = "升级应用", doc = "升级应用")
    public Object upgrade(Records rec) {
        checkLock(rec);
        //先更新依赖再升级
        updateModules(rec);
        List<String> modules = new ArrayList<>();
        for (Records module : rec) {
            if ("installed".equals(module.getString("state"))) {
                module.set("state", "installing");
                modules.add(module.getString("name"));
            }
            modules.addAll(installDepends(module));
        }
        rec.flush();
        String content = String.format("操作人：%s;模块:%s", rec.getEnv().getUser().get("present"), Utils.join(modules));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "升级应用", content, Loader.LogLevel.Info);
        rec.getEnv().getCursor().commit();
        /** 重置租户 */
        resetTenant(rec);
        return Action.js("view.showLog()");
    }

    /**
     * 升级所有模块
     */
    @Model.ServiceMethod(label = "升级所有应用", doc = "升级所有应用", ids = false)
    public Object upgradeAll(Records rec) {
        checkLock(rec);
        //先更新依赖再升级
        updateModules(rec);
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("update ir_module set state='installing' where state='installed'");
        rec.flush();
        String content = String.format("操作人：%s", rec.getEnv().getUser().get("present"));
        Loader.addModuleLog(rec.getEnv().getDatabase(), "升级所有应用", content, Loader.LogLevel.Info);
        cr.commit();
        /** 重置租户 */
        resetTenant(rec);
        return Action.js("view.showLog()");
    }

    public void registerBeans(Records rec, Manifest manifest) {
        for (Class<?> clazz : manifest.beans()) {
            try {
                SpringUtils.registerBean(clazz);
            } catch (Exception e) {
                rec.getLogger().error(String.format("注册Bean[%s]失败", clazz.getName()), e);
            }
        }
        for (Class<?> clazz : manifest.controllers()) {
            try {
                SpringUtils.registerController(clazz);
            } catch (Exception e) {
                rec.getLogger().error(String.format("注册控制器[%s]失败", clazz.getName()), e);
            }
        }
    }

    public void unregisterBeans(Records rec, Manifest manifest) {
        for (Class<?> clazz : manifest.beans()) {
            SpringUtils.unregisterBean(clazz);
        }
        for (Class<?> clazz : manifest.controllers()) {
            SpringUtils.unregisterController(clazz);
        }
    }
}
