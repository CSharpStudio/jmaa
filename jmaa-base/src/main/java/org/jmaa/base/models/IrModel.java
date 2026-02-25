package org.jmaa.base.models;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.util.KvMap;
import org.apache.commons.collections4.SetUtils;

/**
 * 模型元数据
 *
 * @author
 */
@Model.Meta(name = "ir.model", label = "模型元数据")
@Model.Service(remove = {"createBatch", "create", "copy", "importData"})
public class IrModel extends Model {
    static Field name = Field.Char().label("名称").index().required();
    static Field model = Field.Char().label("模型").required().unique();
    static Field description = Field.Char().label("描述").help("模型说明");
    static Field inherit = Field.Char().label("继承").help("模型的继承，多个使用逗号','分隔");
    static Field table = Field.Char().label("表名").help("映射数据库的表名");
    static Field order = Field.Char().label("排序").help("默认排序SQL, 如 name ASC, date DESC");
    static Field present_fields = Field.Char().label("呈现").help("记录呈现的字段名，默认尝试使用name字段呈现");
    static Field is_transient = Field.Boolean().label("是否瞬态");
    static Field is_abstract = Field.Boolean().label("是否抽象").defaultValue(false);
    static Field model_class = Field.Char().label("类").help("对应代码的类");
    static Field field_ids = Field.One2many("ir.model.field", "model_id").label("字段");
    static Field module_id = Field.Many2one("ir.module").label("应用");
    static Field origin = Field.Selection().label("来源").selection(new Options() {{
        put("base", "内置");
        put("manual", "自建");
    }}).defaultValue("manual").readonly();
    static Field table_name = Field.Char().label("表名").compute("computeTable");
    static Field view_ids = Field.One2many("ir.ui.view", "").label("视图").compute("computeView");

    public Object computeView(Records record) {
        return null;
    }

    /**
     * 计算表名
     */
    public String computeTable(Records rec) {
        String tableName = rec.getString("table");
        boolean isAbstract = false;//Func.toBoolean(rec.get(is_abstract));
        if (!isAbstract && Utils.isEmpty(tableName)) {
            tableName = rec.getString("model").replaceAll("\\.", "_");
        }
        return tableName;
    }

    public void addManualModels(Records rec) {
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT * FROM ir_model WHERE origin=%s", Arrays.asList("manual"));
        ModelBuilder builder = Loader.get(ModelBuilder.class);
        for (Map<String, Object> data : cr.fetchMapAll()) {
            addManualModel(rec, builder, data);
        }
    }

    public void addManualModel(Records rec, ModelBuilder builder, Map<String, Object> data) {
        String name = (String) data.get("model");
        String label = (String) data.get("name");
        String desc = (String) data.get("description");
        String inherit = (String) data.get("inherit");
        String order = (String) data.get("order");
        String presentFields = (String) data.get("present_fields");
        Boolean isTransient = (Boolean) data.get("is_transient");
        String table = (String) data.get("table");
        CustomModel cm = new CustomModel(name, inherit, label, desc, order, presentFields, table, isTransient);
        builder.buildModel(rec.getMeta().getRegistry(), cm, "");
    }

    public void reflectModels(Records rec, Collection<String> modelNames, String module) {
        Registry reg = rec.getEnv().getRegistry();
        Map<String, Map<String, Object>> expected = new HashMap<>(16);
        Set<String> cols = null;
        List<String> models = new ArrayList<>();
        String moduleId = rec.getEnv().get("ir.module").find(Criteria.equal("name", module)).getId();
        for (String modelName : modelNames) {
            MetaModel metaModel = reg.get(modelName);
            if (metaModel.isCustom()) {
                continue;
            }
            Map<String, Object> meta = reflectModelParams(rec, metaModel);
            meta.put("module_id", moduleId);
            String model = (String) meta.get("model");
            models.add(model);
            expected.put(model, meta);
            if (cols == null) {
                cols = meta.keySet();
            }
        }
        if (cols == null) {
            return;
        }
        List<Map<String, Object>> rows = search(rec, cols, Criteria.in("model", models), null, null, null);
        Map<String, Map<String, Object>> existing = new HashMap<>(16);
        Map<String, String> modelIds = new HashMap<>(16);
        for (Map<String, Object> kv : rows) {
            Map<String, Object> m = new HashMap<>(kv);
            String model = (String) kv.get("model");
            modelIds.put(model, (String) m.remove("id"));
            existing.put(model, m);
        }
        for (Entry<String, Map<String, Object>> e : expected.entrySet()) {
            Map<String, Object> exist = existing.get(e.getKey());
            Map<String, Object> values = e.getValue();
            String name = "model_" + e.getKey().replaceAll("\\.", "_");
            if (exist == null) {
                rec = rec.create(values);
                rec.getEnv().get("ir.model.data").create(new KvMap()
                    .set("name", name)
                    .set("module", module)
                    .set("model", "ir.model")
                    .set("res_id", rec.getId()));

            } else {
                rec = rec.browse(modelIds.get(e.getKey()));
                if (!e.getValue().equals(exist)) {
                    rec.update(values);
                }
            }
        }
    }

    public Map<String, Object> reflectModelParams(Records rec, MetaModel model) {
        Map<String, Object> result = new HashMap<>(16);
        result.put("model", model.getName());
        result.put("name", model.getLabel());
        result.put("description", model.getDescription());
        result.put("order", model.getOrder());
        result.put("origin", model.isCustom() ? "manual" : "base");
        result.put("is_transient", model.isTransient());
        result.put("is_abstract", model.isAbstract());
        result.put("table", model.isAbstract() ? "" : model.getTable());
        String bases = model.getBases().stream().filter(b -> b.getRegistry() != null && !"base".equals(b.getName())).map(b -> b.getName()).collect(Collectors.joining(","));
        result.put("inherit", bases);
        result.put("present_fields", model.getPresentFormat());
        return result;
    }

    public void addFeatures(Records record, List<String> features) {
        //添加功能
    }

    @ServiceMethod(auth = "read", label = "查询视图", ids = false)
    public Object searchView(Records records, Criteria criteria, Collection<String> fields, Integer offset, Integer limit, String order) {
        return records.getEnv().get("ir.ui.view").searchLimit(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(auth = "update", label = "编辑视图", ids = false)
    public Object updateView(Records records, String viewId, Map<String, Object> values) {
        records.getEnv().get("ir.ui.view", viewId).update(values);
        return Action.success();
    }

    @ServiceMethod(auth = "update", label = "创建视图", ids = false)
    public Object createView(Records records, Map<String, Object> values) {
        Records view = records.getEnv().get("ir.ui.view").create(values);
        return view.getId();
    }

    @ServiceMethod(auth = "update", label = "删除视图", ids = false)
    public Object deleteView(Records records, List<String> viewIds) {
        records.getEnv().get("ir.ui.view", viewIds).delete();
        return Action.success();
    }
}
