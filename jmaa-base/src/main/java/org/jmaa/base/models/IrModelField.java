package org.jmaa.base.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.*;
import org.jmaa.sdk.fields.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.util.KvMap;

/**
 * 字段
 *
 * @author
 */
@Model.Meta(name = "ir.model.field", label = "字段", authModel = "ir.model", order = "seq")
@Model.UniqueConstraint(name = "unique_model_name", fields = {"model_id", "name", "origin"}, message = "名称不能重复")
public class IrModelField extends Model {
    static Map<String, String> FIELD_TYPES = Field.getFieldTypes().stream().collect(Collectors.toMap(k -> k, v -> v));

    static Field name = Field.Char().label("名称").help("字段名").index(true).required(true);
    static Field model_id = Field.Many2one("ir.model").label("模型").required().index().help("字段所属的模型");
    static Field field_type = Field.Selection(FIELD_TYPES).label("类型").help("字段类型").required(true)
        .defaultValue("char");
    static Field relation = Field.Char().label("关联的模型");
    static Field relation_field = Field.Char().label("一对多关联字段");
    static Field label = Field.Char().label("标题");
    static Field help = Field.Char().label("帮助");
    static Field related = Field.Char().label("关联");
    static Field required = Field.Boolean().label("是否必填");
    static Field readonly = Field.Boolean().label("是否只读");
    static Field index = Field.Boolean().label("是否索引");
    static Field translate = Field.Boolean().label("是否翻译");
    static Field length = Field.Integer().label("最大长度").defaultValue(240);
    static Field origin = Field.Selection(new Options() {{
        put("manual", "自建");
        put("base", "内置");
    }}).label("来源").defaultValue("manual").readonly();
    static Field on_delete = Field.Selection(new Options() {{
        put("Cascade", "级联");
        put("SetNull", "设置为null");
        put("Restrict", "限制");
    }}).label("删除操作").defaultValue("Restrict");
    static Field auth = Field.Boolean().label("是否需要权限访问");
    static Field relation_table = Field.Char().label("多对多关联表");
    static Field column1 = Field.Char().label("左联字段");
    static Field column2 = Field.Char().label("右联字段");
    static Field compute = Field.Char().label("计算表达式");
    static Field default_value = Field.Char().label("默认值");
    static Field depends = Field.Char().label("依赖");
    static Field store = Field.Boolean().label("是否存储").defaultValue(true);
    static Field copy = Field.Boolean().label("是否复制");
    static Field selection_ids = Field.One2many("ir.model.field.selection", "field_id").label("选项");
    static Field seq = Field.Integer().label("顺序");
    static Field is_catalog = Field.Boolean().label("是否快码").help("选择字段是否可以通过快码维护选项").defaultValue(false);


    public void addManualFields(Records rec, MetaModel model) {
        Environment env = rec.getEnv();
        Map<String, List<Map<String, Object>>> cache = (Map<String, List<Map<String, Object>>>) env.getContext().get("manual-fields");
        if (cache == null) {
            Cursor cr = rec.getEnv().getCursor();
            cr.execute("SELECT f.*,m.model FROM ir_model_field f JOIN ir_model m ON f.model_id=m.id WHERE f.origin='manual'");
            List<Map<String, Object>> list = cr.fetchMapAll();
            cache = list.stream().collect(Collectors.groupingBy(row -> (String) row.get("model")));
            env.getContext().put("manual-fields", cache);
        }

        List<Map<String, Object>> fieldList = cache.get(model.getName());
        if (fieldList != null) {
            MetaModelWrapper wrapper = new MetaModelWrapper(model);
            for (Map<String, Object> row : fieldList) {
                addManualField(rec, wrapper, row);
            }
        }
    }

    public void addManualField(Records records, MetaModelWrapper wrapper, Map<String, Object> data) {
        MetaField field = Field.create((String) data.get("field_type"));
        Map<String, Object> args = getArgs(data);
        field.setArgs(args);
        field.setName((String) data.get("name"));
        wrapper.addField(field.getName(), field);
    }

    void putIf(KvMap map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Map<String, Object> getArgs(Map<String, Object> fieldData) {
        KvMap attrs = new KvMap(fieldData.size());
        attrs.put("manual", true);
        putIf(attrs, "label", fieldData.get("label"));
        putIf(attrs, "help", fieldData.get("help"));
        putIf(attrs, "index", fieldData.get("index"));
        putIf(attrs, "copy", fieldData.get("copy"));
        putIf(attrs, "required", fieldData.get("required"));
        putIf(attrs, "readonly", fieldData.get("readonly"));
        putIf(attrs, "store", fieldData.get("store"));

        String related = (String) fieldData.get("related");
        if (Utils.isNotBlank(related)) {
            putIf(attrs, "related", related.split("\\."));
        }

        String type = (String) fieldData.get("field_type");
        if (Constants.CHAR.equals(type) || Constants.TEXT.equals(type) || Constants.HTML.equals(type)) {
            putIf(attrs, "translate", fieldData.get("translate"));
            if (Constants.CHAR.equals(type)) {
                putIf(attrs, "length", fieldData.get("length"));
            }
        } else if (Constants.SELECTION.equals(type)) {

        } else if (Constants.MANY2ONE.equals(type)) {
            putIf(attrs, "comodelName", fieldData.get("relation"));
            String ondelete = (String) fieldData.get("on_delete");
            if (Utils.isEmpty(ondelete)) {
                ondelete = "SetNull";
            }
            putIf(attrs, "ondelete", DeleteMode.valueOf(ondelete));
        } else if (Constants.ONE2MANY.equals(type)) {
            putIf(attrs, "comodelName", fieldData.get("relation"));
            putIf(attrs, "inverseName", fieldData.get("relation_field"));
        } else if (Constants.MANY2MANY.equals(type)) {
            putIf(attrs, "comodelName", fieldData.get("relation"));
            putIf(attrs, "relation", fieldData.get("relation_table"));
            putIf(attrs, "column1", fieldData.get("column1"));
            putIf(attrs, "column2", fieldData.get("column2"));
        }
        String compute = (String) fieldData.get("compute");
        if (Utils.isNotBlank(compute)) {
            putIf(attrs, "compute", Callable.script(compute));
        }
        return attrs;
    }

    public void reflectFields(Records rec, Collection<String> modelNames, String module) {
        Map<String, Map<String, Object>> expected = new LinkedHashMap<>(16);
        Set<String> cols = null;
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT id,model FROM ir_model WHERE model in %s", Arrays.asList(modelNames));
        Map<String, String> modelIdMap = cr.fetchAll().stream().collect(Collectors.toMap(r -> (String) r[1], r -> (String) r[0]));
        for (String modelName : modelNames) {
            Object modelId = modelIdMap.get(modelName);
            MetaModel model = rec.getEnv().getRegistry().get(modelName);
            List<String> fields = new ArrayList<>();
            for (MetaField field : model.getFields().values()) {
                if (field.isManual()) {
                    continue;
                }
                Map<String, Object> meta = reflectFieldsParams(rec, field, modelId);
                String name = (String) meta.get("name");
                fields.add(name);
                expected.put(modelName + "#" + name, meta);
                if (cols == null) {
                    cols = meta.keySet();
                }
            }
        }
        if (cols == null) {
            return;
        }
        String colNames = cols.stream().map(c -> "f." + cr.quote(c)).collect(Collectors.joining(","));
        String sql = String.format(
            "SELECT %s, m.model, f.id FROM ir_model_field f join ir_model m on f.model_id=m.id WHERE m.model IN %%s",
            colNames);
        cr.execute(sql, Arrays.asList(modelNames));
        List<Map<String, Object>> rows = cr.fetchMapAll();
        Map<String, Map<String, Object>> existing = new HashMap<>(rows.size());
        Map<String, String> ids = new HashMap<>(rows.size());
        for (Map<String, Object> kv : rows) {
            Map<String, Object> m = new HashMap<>(kv);
            ids.put(kv.get("model") + "#" + kv.get("name"), (String) kv.get("id"));
            m.remove("id");
            m.remove("model");
            existing.put(kv.get("model") + "#" + kv.get("name"), m);
        }
        for (Entry<String, Map<String, Object>> e : expected.entrySet()) {
            Map<String, Object> exist = existing.get(e.getKey());
            Map<String, Object> values = e.getValue();
            if (exist == null) {
                String name = "field_" + e.getKey().replaceAll("\\.", "_");
                try {
                    rec = rec.create(values);
                }
                catch (Exception ex){
                    System.out.println(ex);
                }
                rec.getEnv().get("ir.model.data").create(new KvMap()
                    .set("name", name)
                    .set("module", module)
                    .set("model", "ir.model.field")
                    .set("res_id", rec.getId()));
            } else if (!values.equals(exist)) {
                rec = rec.browse(ids.get(e.getKey()));
                rec.update(values);
            }
        }
    }

    public Map<String, Object> reflectFieldsParams(Records rec, MetaField field, Object modelId) {
        Map<String, Object> result = new HashMap<>(16);
        result.put("model_id", modelId);
        result.put("name", field.getName());
        result.put("label", field.getLabel());
        result.put("help", field.getHelp());
        result.put("field_type", field.getType());
        result.put("origin", field.isManual() ? "manual" : "base");
        String relation = null;
        if (field instanceof RelationalField) {
            relation = ((RelationalField<?>) field).getComodel();
        }
        result.put("relation", relation);
        result.put("index", field.isIndex());
        result.put("store", field.isStore());
        result.put("copy", field.isCopy());
        String onDelete = null;
        if (field instanceof Many2oneField) {
            onDelete = ((Many2oneField) field).getOnDelete().name();
        }
        result.put("on_delete", onDelete);
        String related = String.join(".", field.getRelated());
        if (Utils.isEmpty(related)) {
            related = null;
        }
        result.put("related", related);
        result.put("readonly", field.isReadonly());
        result.put("required", field.isRequired());
        Integer length = null;
        if (field instanceof CharField) {
            length = ((CharField) field).getLength();
        }
        result.put("length", length);
        Boolean translate = false;
        if (field instanceof StringField) {
            translate = ((StringField<?>) field).isTranslate();
        }
        result.put("translate", translate);
        String relationField = null, relationTable = null, column1 = null, column2 = null;
        if (field instanceof One2manyField) {
            relationField = ((One2manyField) field).getInverseName();
        }
        if (field instanceof Many2manyField) {
            Many2manyField m2m = (Many2manyField) field;
            relationTable = m2m.getRelation();
            column1 = m2m.getColumn1();
            column2 = m2m.getColumn2();
        }

        result.put("relation_field", relationField);
        result.put("relation_table", relationTable);
        result.put("column1", column1);
        result.put("column2", column2);
        result.put("seq", field.getSequence());
        if (field instanceof SelectionField) {
            result.put("is_catalog", ((SelectionField) field).isCatalog());
        }

        return result;
    }
}
