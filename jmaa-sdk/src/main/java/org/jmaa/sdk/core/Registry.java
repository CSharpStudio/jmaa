package org.jmaa.sdk.core;

import org.jmaa.sdk.data.ColumnType;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.data.SqlDialect;
import org.jmaa.sdk.data.xml.SqlTemplate;
import org.jmaa.sdk.fields.Many2oneField;
import org.jmaa.sdk.tenants.Tenant;
import org.jmaa.sdk.tools.ObjectUtils;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.Tuple;
import org.jmaa.sdk.util.Tuple4;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.exceptions.ModelException;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 模型注册表
 *
 * @author Eric Liang
 */
public class Registry {
    Map<String, MetaModule> modules = new LinkedHashMap<>();
    Map<String, MetaModel> map = new HashMap<>();
    List<Consumer<Environment>> postInit;
    List<Runnable> onClose;
    Tenant tenant;
    boolean loaded = false;
    boolean closed = false;
    Map<Tuple<String, String>, KvMap> foreignKeys;
    List<String> newModels = new ArrayList<>();
    ConcurrentHashMap<String, FutureTask<SqlTemplate>> templates = new ConcurrentHashMap<>();

    /**
     * 注册表是否加载完成
     */
    public boolean isLoaded() {
        return loaded;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * 获取租户，注册表与租户一一对应，用于隔离每个租户安装的不同应用
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * 获取所有元模型
     */
    public Map<String, MetaModel> getModels() {
        return map;
    }

    /**
     * 获取所有模块
     */
    public Map<String, MetaModule> getModules() {
        return modules;
    }

    /**
     * 获取Sql模板
     */
    public ConcurrentHashMap<String, FutureTask<SqlTemplate>> getTemplates() {
        return templates;
    }

    /**
     * 构建注册表实例
     *
     * @param tenant 注册表对应的租户
     */
    public Registry(Tenant tenant) {
        this.tenant = tenant;
    }

    /**
     * 关闭注册表
     */
    public void close() {
        closed = true;
        if (onClose != null) {
            for (Runnable r : onClose) {
                r.run();
            }
        }
    }

    /**
     * 添加初始化操作
     */
    public void addPostInit(Consumer<Environment> init) {
        if (postInit == null) {
            postInit = new ArrayList<>();
        }
        postInit.add(init);
    }

    public void addOnClose(Runnable runnable) {
        if (onClose == null) {
            onClose = new ArrayList<>();
        }
        onClose.add(runnable);
    }

    public void put(String model, MetaModel meta) {
        map.put(model, meta);
    }

    /**
     * 获取元模型
     *
     * @param model 模型名称
     */
    public MetaModel get(String model) {
        MetaModel m = map.get(model);
        if (m == null) {
            throw new ModelException("模型[" + model + "]未注册");
        }
        return m;
    }

    /**
     * 判断是否包含元模型
     *
     * @param model 模型名称
     */
    public boolean contains(String model) {
        return map.containsKey(model);
    }

    void addUserRoot(Environment env) {
        Records root = env.findRef("base.user_root");
        if (root == null) {
            Records company = env.get("res.company").create(new HashMap<String, Object>() {{
                put("name", "我的公司");
                put("inv_org", "01");
                put("org_code", "M");
            }});
            root = env.get("rbac.user").create(new HashMap<String, Object>() {{
                put("id", "__system__");
                put("login", "__system__");
                put("name", "系统管理员");
                put("tz", "Asia/Shanghai");
                put("lang", "zh_CN");
                put("active", false);
                put("company_ids", Collections.singletonList(Arrays.asList(4, company.getId(), 0)));
                put("company_id", company.getId());
            }});
            env.get("ir.model.data").create(new KvMap()
                .set("name", "user_root")
                .set("module", "base")
                .set("model", "rbac.user")
                .set("res_id", root.getId()));
            env.get("ir.model.data").create(new KvMap()
                .set("name", "company_default")
                .set("module", "base")
                .set("model", "res.company")
                .set("res_id", company.getId()));
        }
    }

    /**
     * 初始化模型。auto=true的模型自动更新数据库表
     *
     * @param env    数据库游标
     * @param models 需要初始化的模型列表
     * @param init   是否初始化数据，第一次创建数据库时，创建根用户数据并更新模块信息
     */
    public void initModels(Environment env, Collection<String> models, String module, boolean init) {
        for (String model : models) {
            MetaModel m = get(model);
            m.autoInit(env);
        }
        if (init) {
            addUserRoot(env);
            env.get("ir.module").call("updateModules");
        }
        env.get("ir.model").call("reflectModels", models, module);
        env.get("ir.model.field").call("reflectFields", models, module);
        env.get("ir.model.field.selection").call("reflectSelections", models, module);
        env.get("ir.model.constraint").call("reflectModels", models, module);
        newModels.addAll(models);
    }

    public void postInit(Environment env) {
        if (postInit != null) {
            for (Consumer<Environment> c : postInit) {
                c.accept(env);
            }
        }
        checkForeignKeys(env);
        checkIndexes(env, newModels);
        newModels.clear();
        newModels = null;
        if (foreignKeys != null) {
            foreignKeys.clear();
            foreignKeys = null;
        }
        if (postInit != null) {
            postInit.clear();
            postInit = null;
        }
    }

    void checkIndexes(Environment env, Collection<String> models) {
        Map<String, Object[]> expected = new HashMap<>();
        SqlDialect sd = env.getCursor().getSqlDialect();
        for (String modelName : models) {
            MetaModel model = env.getRegistry().get(modelName);
            if (model.isAuto() && !model.isAbstract()) {
                for (MetaField field : model.getFields().values()) {
                    if (field.getColumnType() != ColumnType.None && field.isStore() && !(field instanceof Many2oneField)) {
                        String idx = sd.limitIdentity(String.format("idx_%s_%s", model.getTable(), field.getName()));
                        expected.put(idx, new Object[]{model.getTable(), field.getName(), field.isIndex()});
                    }
                }
            }
        }
        if (expected.isEmpty()) {
            return;
        }
        Map<String, String> existing = sd.getIndexes(env.getCursor(), expected.keySet());
        for (Entry<String, Object[]> e : expected.entrySet()) {
            if (!existing.containsKey(e.getKey())) {
                if (ObjectUtils.toBoolean(e.getValue()[2])) {
                    sd.createIndex(env.getCursor(), e.getKey(), (String) e.getValue()[0], Collections.singletonList((String) e.getValue()[1]));
                }
            } else if (!ObjectUtils.toBoolean(e.getValue()[2])) {
                if (Objects.equals(e.getValue()[0], existing.get(e.getKey()))) {
                    SqlDialect.schema.info("Keep unexpected index {} on table {}", e.getKey(), e.getValue()[0]);
                }
            }
        }
    }

    /**
     * 添加外键信息，在所有数据表更新后，再执行外键的更新
     */
    public void addForeignKey(String table1, String column1, String table2, String column2, String ondelete,
                              String model, String module, boolean force) {
        if (foreignKeys == null) {
            foreignKeys = new HashMap<>();
        }
        Tuple<String, String> key = new Tuple<>(table1, column1);
        KvMap map = new KvMap()
            .set("table2", table2)
            .set("column2", column2)
            .set("ondelete", ondelete)
            .set("model", model)
            .set("module", module);
        if (force) {
            foreignKeys.put(key, map);
        } else {
            foreignKeys.putIfAbsent(key, map);
        }
    }

    void checkForeignKeys(Environment env) {
        if (foreignKeys == null || foreignKeys.size() == 0) {
            return;
        }
        Cursor cr = env.getCursor();
        Collection<String> tables = foreignKeys.keySet().stream().map(t -> t.getItem1()).collect(Collectors.toSet());
        SqlDialect sd = cr.getSqlDialect();
        List<Object[]> rows = sd.getForeignKeys(cr, tables);
        Map<Tuple<String, String>, Tuple4<String, String, String, String>> existing = new HashMap<>();
        for (Object[] row : rows) {
            existing.put(new Tuple<>((String) row[1], (String) row[2]),
                new Tuple4<>((String) row[0], (String) row[3], (String) row[4], (String) row[5]));
        }
        for (Entry<Tuple<String, String>, KvMap> kv : foreignKeys.entrySet()) {
            String table1 = kv.getKey().getItem1();
            String column1 = kv.getKey().getItem2();
            String table2 = (String) kv.getValue().get("table2");
            String column2 = (String) kv.getValue().get("column2");
            String ondelete = (String) kv.getValue().get("ondelete");
            String model = (String) kv.getValue().get("model");
            String module = (String) kv.getValue().get("module");
            Tuple4<String, String, String, String> spec = existing.get(kv.getKey());
            if (spec == null) {
                String fk = sd.addForeignKey(cr, table1, column1, table2, column2, ondelete);
                env.get("ir.model.constraint").call("reflectConstraint", fk, "f", ondelete, model, model, module);
            } else if (!table2.equals(spec.getItem2()) || !column2.equals(spec.getItem3())
                || !ondelete.equals(spec.getItem4())) {
                sd.dropConstraint(cr, table1, spec.getItem1());
                String fk = sd.addForeignKey(cr, table1, column1, table2, column2, ondelete);
                env.get("ir.model.constraint").call("reflectConstraint", fk, "f", ondelete, model, model, module);
            }
        }
    }

    /**
     * 设置模型，在初始化前完成模型的设置
     */
    public void setupModels(Cursor cr) {
        Environment env = new Environment(this, cr, Constants.SYSTEM_USER);
        // TODO reset

        // TODO add manual models
        if (!modules.isEmpty()) {
            env.get("ir.model").call("addManualModels");
        }

        Collection<MetaModel> models = getModels().values();
        for (MetaModel model : models) {
            model.setupBase(env);
        }

        for (MetaModel model : models) {
            model.setupFields();
        }

        for (MetaModel model : models) {
            model.setupComplete(env);
        }
    }
}
