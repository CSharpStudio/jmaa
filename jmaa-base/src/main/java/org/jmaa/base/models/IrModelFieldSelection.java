package org.jmaa.base.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.core.MetaModel;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.Tuple;

import java.util.*;

/**
 * 模型字段选择
 *
 * @author Eric Liang
 */
@Model.UniqueConstraint(name = "field_value_unique", fields = {"field_id", "value"})
@Model.Meta(name = "ir.model.field.selection", order = "sequence,id", label = "字段的选项", authModel = "ir.model.field")
public class IrModelFieldSelection extends Model {
    static Field field_id = Field.Many2one("ir.model.field").label("字段").required().ondelete(DeleteMode.Cascade)
        .index().lookup(Criteria.in("field_type", "selection"));
    static Field value = Field.Char().label("值").required();
    static Field name = Field.Char().label("名称");
    static Field sequence = Field.Integer().defaultValue(1000).label("显示顺序");
    static Field origin = Field.Selection(new Options() {{
        put("base", "内置");
        put("manual", "自建");
    }}).label("来源").defaultValue("manual").readonly().required();
    static Field active = Field.Boolean().label("是否生效").defaultValue(true);

    /**
     * 获取字段的选项，不要在代码中直接调用此方法，应该使用模型的元数据获取选项
     */
    public Map<String, String> getSelection(Records rec, String model, String field) {
        String sql = "SELECT s.value, s.name\n" +
            " FROM ir_model_field_selection s, ir_model_field f, ir_model m\n" +
            " WHERE s.field_id = f.id AND f.model_id=m.id AND s.active=%s AND m.model=%s AND f.name=%s\n" +
            " ORDER BY s.sequence";
        Cursor cr = rec.getEnv().getCursor();
        cr.execute(sql, Arrays.asList(true, model, field));
        Map<String, String> result = new LinkedHashMap<>();
        for (Object[] row : cr.fetchAll()) {
            result.put((String) row[0], (String) row[1]);
        }
        return result;
    }

    public void reflectSelections(Records rec, Collection<String> modelNames, String module) {
        List<SelectionField> fields = new ArrayList<>();
        for (String modelName : modelNames) {
            MetaModel model = rec.getEnv().getRegistry().get(modelName);
            for (MetaField field : model.getFields().values()) {
                if (field instanceof SelectionField) {
                    SelectionField sf = (SelectionField) field;
                    if (sf.isStatic()) {
                        fields.add(sf);
                    }
                }
            }
        }
        if (fields.isEmpty()) {
            return;
        }
        Map<Tuple, Tuple> expected = new HashMap<>();
        Map<String, Object[]> fieldMap = new HashMap<>();
        Records irField = rec.getEnv().get("ir.model.field");
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT f.id,m.model,f.name,f.origin FROM ir_model_field f, ir_model m WHERE f.model_id=m.id AND m.model in %s order by origin", Arrays.asList(modelNames));
        Map<String, String> fieldIdMap = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            fieldIdMap.put(row[1] + ":" + row[2], (String) row[0]);
        }
        for (SelectionField f : fields) {
            String fieldId = fieldIdMap.get(f.getModelName() + ":" + f.getName());
            fieldMap.put(fieldId, new Object[]{f.getModelName(), f.getName()});
            int index = 0;
            for (Map.Entry<String, String> e : f.getOptions(rec).entrySet()) {
                Tuple k = new Tuple(fieldId, e.getKey());
                Tuple v = new Tuple(e.getValue(), index++);
                expected.put(k, v);
            }
        }

        String sql = "SELECT s.field_id, s.value, s.name, s.sequence, s.id\n" +
            "            FROM ir_model_field_selection s, ir_model_field f, ir_model m\n" +
            "            WHERE s.field_id = f.id AND f.model_id=m.id AND s.origin='base' AND m.model IN %s";
        cr.execute(sql, Arrays.asList(modelNames));
        Map<Tuple, Tuple> existing = new HashMap<>();
        Map<Tuple, String> ids = new HashMap<>();
        for (Object[] row : cr.fetchAll()) {
            existing.put(new Tuple(row[0], row[1]), new Tuple(row[2], row[3]));
            ids.put(new Tuple(row[0], row[1]), (String) row[4]);
        }

        for (Map.Entry<Tuple, Tuple> e : expected.entrySet()) {
            Tuple exist = existing.get(e.getKey());
            if (exist == null) {
                rec = rec.create(new HashMap<String, Object>() {{
                    put("field_id", e.getKey().getItem1());
                    put("value", e.getKey().getItem2());
                    put("name", e.getValue().getItem1());
                    put("origin", "base");
                    put("sequence", e.getValue().getItem2());
                }});
                rec.getEnv().get("ir.model.data").create(new KvMap()
                    .set("name", String.format("selection-%s-%s", fieldMap.get(e.getKey().getItem1())) + "-" + e.getKey().getItem2())
                    .set("module", module)
                    .set("model", "ir.model")
                    .set("res_id", rec.getId()));
            } else if (!e.getValue().equals(exist)) {
                rec.browse(ids.get(e.getKey())).update(new HashMap<String, Object>() {{
                    put("field_id", e.getKey().getItem1());
                    put("value", e.getKey().getItem2());
                    put("name", e.getValue().getItem1());
                    put("sequence", e.getValue().getItem2());
                }});
            }
        }
    }
}
