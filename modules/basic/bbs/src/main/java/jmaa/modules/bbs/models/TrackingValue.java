package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.MetaField;
import org.jmaa.sdk.exceptions.ValueException;
import org.jmaa.sdk.fields.SelectionField;
import org.jmaa.sdk.util.KvMap;

import java.util.*;

@Model.Meta(name = "bbs.tracking_value", label = "跟踪值")
public class TrackingValue extends Model {
    static Field field_type = Field.Char().label("字段类型").readonly();
    static Field field_label = Field.Char().label("字段标题").readonly();
    static Field field_name = Field.Char().label("字段名称").readonly();
    static Field old_value_integer = Field.Integer().label("旧整数值").readonly();
    static Field old_value_float = Field.Float().label("旧小数值").readonly();
    static Field old_value_char = Field.Char().label("旧字符值").readonly();
    static Field old_value_text = Field.Text().label("旧长文本值").readonly();
    static Field old_value_datetime = Field.DateTime().label("旧日期值").readonly();
    static Field new_value_integer = Field.Integer().label("新整数值").readonly();
    static Field new_value_float = Field.Float().label("新小数值").readonly();
    static Field new_value_char = Field.Char().label("新字符值").readonly();
    static Field new_value_text = Field.Text().label("新长文本值").readonly();
    static Field new_value_datetime = Field.DateTime().label("新日期值").readonly();
    static Field message_id = Field.Many2one("bbs.message").label("消息").ondelete(DeleteMode.Cascade);

    public Map createTrackingValues(Records records, Object initialValue, Object newValue, MetaField field) {
        Map values = new KvMap();
        values.put("field_name", field.getName());
        values.put("field_type", field.getType());
        values.put("field_label", field.getLabel());
        if (Utils.asList("integer", "float", "char", "text", "datetime").contains(field.getType())) {
            values.put("old_value_" + field.getType(), initialValue);
            values.put("new_value_" + field.getType(), newValue);
        } else if (Utils.equals("date", field.getType())) {
            values.put("old_value_datetime", initialValue);
            values.put("new_value_datetime", newValue);
        } else if (Utils.equals("boolean", field.getType())) {
            values.put("old_value_integer", Utils.toBoolean(initialValue) ? 1 : 0);
            values.put("new_value_integer", Utils.toBoolean(newValue) ? 1 : 0);
        } else if (Utils.equals("selection", field.getType())) {
            Map<String, String> options = ((SelectionField) field).getOptions(records);
            values.put("old_value_char", options.get(initialValue));
            values.put("new_value_char", options.get(newValue));
        } else if (Utils.equals("many2one", field.getType())) {
            if (initialValue instanceof Records) {
                values.put("old_value_char", ((Records) initialValue).get("present"));
            }
            if (newValue instanceof Records) {
                values.put("new_value_char", ((Records) newValue).get("present"));
            }
        } else if (Utils.equals("one2many", field.getType()) || Utils.equals("many2many", field.getType())) {
            if (initialValue instanceof Records) {
                List<String> msg = new ArrayList<>();
                ((Records) initialValue).forEach(r -> msg.add(r.getString("present")));
                values.put("old_value_char", Utils.join(msg));
            }
            if (newValue instanceof Records) {
                List<String> msg = new ArrayList<>();
                ((Records) newValue).forEach(r -> msg.add(r.getString("present")));
                values.put("new_value_char", Utils.join(msg));
            }
        } else {
            throw new ValueException(String.format("不支持字段[%s]跟踪", field));
        }
        return values;
    }

    public List<Map<String, Object>> trackingValueFormat(Records records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records row : records) {
            String type = row.getString("field_type");
            KvMap map = new KvMap() {{
                put("fieldLabel", row.get("field_label"));
                put("fieldName", row.get("field_name"));
                put("fieldType", type);
                put("newValue", getDisplayValue(row, type, true));
                put("oldValue", getDisplayValue(row, type, false));
            }};
            result.add(map);
        }
        return result;
    }

    static Object getDisplayValue(Records record, String fieldType, boolean isNew) {
        Map<String, String> mapping = new HashMap<String, String>() {{
            put("boolean", "_value_integer");
            put("date", "_value_datetime");
            put("datetime", "_value_datetime");
            put("char", "_value_char");
            put("selection", "_value_char");
            put("float", "_value_float");
            put("integer", "_value_integer");
            put("text", "_value_text");
        }};
        String field = (isNew ? "new" : "old") + mapping.getOrDefault(fieldType, "_value_char");
        Object value = record.get(field);
        if ("boolean".equals(fieldType)) {
            value = Utils.toBoolean(value);
        }
        if ("date".equals(fieldType) && value instanceof Date) {
            value = Utils.format((Date) value, "yyyy-MM-dd");
        }
        if ("datetime".equals(fieldType) && value instanceof Date) {
            value = Utils.format((Date) value, "yyyy-MM-dd HH:mm:ss");
        }
        return value;
    }
}
