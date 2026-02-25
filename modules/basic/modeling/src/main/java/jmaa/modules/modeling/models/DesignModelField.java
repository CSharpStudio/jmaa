package jmaa.modules.modeling.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.TextBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模型字段
 *
 * @author 梁荣振
 */
@Model.Meta(name = "modeling.model.field", label = "字段", authModel = "modeling.model", order = "id")
public class DesignModelField extends Model {
    static Map<String, String> FIELD_TYPES = Field.getFieldTypes().stream().collect(Collectors.toMap(k -> k, v -> v));
    static Map<String, String> BOOLEAN = new Options() {{
        put("@", "继承");
        put("false", "否");
        put("true", "是");
    }};

    static Field name = Field.Char().label("名称").help("字段名").index().required();
    static Field model_id = Field.Many2one("modeling.model").label("模型").required().index().help("字段所属的模型")
        .ondelete(DeleteMode.Cascade);
    static Field field_type = Field.Selection(FIELD_TYPES).label("类型").help("字段类型").required()
        .defaultValue("char");
    static Field relation = Field.Char().label("关联的模型");
    static Field relation_field = Field.Char().label("一对多关联字段");
    static Field label = Field.Char().label("标题");
    static Field help = Field.Char().label("帮助");
    static Field related = Field.Char().label("关联");
    static Field required = Field.Selection(BOOLEAN).label("是否必填").defaultValue("@");
    static Field readonly = Field.Selection(BOOLEAN).label("是否只读").defaultValue("@");
    static Field index = Field.Selection(BOOLEAN).label("是否索引").defaultValue("@");
    static Field translate = Field.Selection(BOOLEAN).label("是否翻译").defaultValue("@");
    static Field length = Field.Integer().label("最大长度");
    static Field on_delete = Field.Selection(new Options() {{
        put("@", "继承");
        put("Cascade", "级联");
        put("SetNull", "设置为null");
        put("Restrict", "限制");
    }}).label("删除操作").defaultValue("@");
    static Field auth = Field.Selection(BOOLEAN).label("是否需要权限访问").defaultValue("@");
    static Field relation_table = Field.Char().label("多对多关联表");
    static Field column1 = Field.Char().label("左联字段");
    static Field column2 = Field.Char().label("右联字段");
    static Field compute = Field.Char().label("计算表达式");
    static Field default_value = Field.Char().label("默认值");
    static Field depends = Field.Char().label("依赖");
    static Field store = Field.Selection(BOOLEAN).label("是否存储").defaultValue("@");
    static Field copy = Field.Selection(BOOLEAN).label("是否复制").defaultValue("@");
    static Field selection_values = Field.Text().label("下拉框内容:请按照键值对输入,示例: <br/>name:xiaoli <br/> age:15<br/>");

    public Map<String, String> getCode(Records rec) {
        rec.ensureOne();
        Map<String, String> result = new HashMap<>(2);
        result.put("field", getFieldCode(rec));
        result.put("method", getMethodCode(rec));
        return result;
    }

    String formatName(String name) {
        String result = "";
        for (String n : name.split("_")) {
            result += Character.toUpperCase(n.charAt(0));
            if (n.length() > 1) {
                result += n.substring(1);
            }
        }
        return result;
    }

    String getMethodCode(Records rec) {
        TextBuilder sb = new TextBuilder();
        String type = (String) rec.get("field_type");
        String javaClass = Field.getJavaClass(type).getSimpleName();
        String name = formatName(rec.getString("name"));
        sb.appendLine("    /** 获取%s */", rec.get("label"));
        sb.appendLine("    public static %s %s%s(Records rec){", javaClass,
            "Boolean".equals(javaClass) ? "is" : "get", name);
        sb.appendLine("        return (%s)rec.get(%s);", javaClass, rec.getString("name"));
        sb.appendLine("    }");
        sb.appendLine();
        sb.appendLine("    /** 设置%s */", rec.get("label"));
        sb.appendLine("    public static void %s%s(Records rec, %s value){", "set", name, javaClass);
        sb.appendLine("        rec.set(%s, value);", rec.getString("name"));
        sb.appendLine("    }");
        return sb.toString();
    }

    @SuppressWarnings("AlibabaMethodTooLong")
    String getFieldCode(Records rec) {
        TextBuilder sb = new TextBuilder();
        String type = (String) rec.get("field_type");
        System.out.println("type = " + type);
        sb.append("\tstatic Field %s = Field.%s(", rec.getString("name"), Field.getFieldName(type).replace("Field", ""));
        if (Constants.MANY2ONE.equals(type)) {
            sb.append("\"%s\")", rec.get("relation"));
            String ondelete = (String) rec.get("on_delete");
            if (!"@".equals(ondelete)) {
                sb.append(".ondelete(DeleteMode.%s)", ondelete);
            }
        } else if (Constants.ONE2MANY.equals(type)) {
            sb.append("\"%s\", \"%s\")", rec.get("relation"), rec.get("relation_field"));
        } else if (Constants.MANY2MANY.equals(type)) {
            sb.append("\"%s\", \"%s\", \"%s\", \"%s\")", rec.get("relation"), rec.get("relation_table"),
                rec.get("column1"), rec.get("column2"));
            String ondelete = (String) rec.get("on_delete");
            if (!"@".equals(ondelete)) {
                sb.append(".ondelete(DeleteMode.%s)", ondelete);
            }
        } else if (Constants.SELECTION.equals(type)) {
            sb.append("new Optionss(){{\n");
            sb.append(parseSelections(rec));
            sb.append("\t}})");
        } else {
            sb.append(")");
        }
        String label = (String) rec.get("label");
        if (Utils.isNotEmpty(label)) {
            sb.append(".label(\"%s\")", label);
        }
        String help = (String) rec.get("help");
        if (Utils.isNotEmpty(help)) {
            sb.append(".help(\"%s\")", help);
        }
        String related = (String) rec.get("related");
        if (Utils.isNotEmpty(related)) {
            sb.append(".related(\"%s\")", related);
        }
        String required = (String) rec.get("required");
        if (!"@".equals(required)) {
            sb.append(".required(%s)", required);
        }
        String readonly = (String) rec.get("readonly");
        if (!"@".equals(readonly)) {
            sb.append(".readonly(%s)", readonly);
        }
        String translate = (String) rec.get("translate");
        if (!"@".equals(translate)) {
            sb.append(".translate(%s)", translate);
        }
        String index = (String) rec.get("index");
        if (!"@".equals(index)) {
            sb.append(".index(%s)", index);
        }
        String store = (String) rec.get("store");
        if (!"@".equals(store)) {
            sb.append(".store(%s)", store);
        }
        String copy = (String) rec.get("copy");
        if (!"@".equals(copy)) {
            sb.append(".copy(%s)", copy);
        }
        String auth = (String) rec.get("auth");
        if (!"@".equals(auth)) {
            sb.append(".auth(%s)", auth);
        }
        String compute = (String) rec.get("compute");
        if (Utils.isNotEmpty(compute)) {
            sb.append(".compute(Callable.script(\"%s\"))", compute);
        }
        String defaultValue = (String) rec.get("default_value");
        if (Utils.isNotEmpty(defaultValue)) {
            if (Constants.CHAR.equals(type) || Constants.TEXT.equals(type) || Constants.HTML.equals(type)) {
                defaultValue = "\"" + defaultValue + "\"";
            }
            sb.append(".defaultValue(%s)", defaultValue);
        }
        if (Constants.CHAR.equals(type)) {
            Integer length = (Integer) rec.get("length");
            if (length != null) {
                sb.append(".length(%s)", length);
            }
        }
        sb.append(";");
        return sb.toString();
    }

    private String parseSelections(Records rec) {
        rec.ensureOne();
        String values = (String) rec.get("selection_values");
        String[] split = values.split("\n");
        System.out.println("values = " + values);
        StringBuilder sb = new StringBuilder();
        if (Utils.isNotBlank(values)) {
            try {
                for (int i = 0; i < split.length; i++) {
                    String[] kvs = split[i].split(":");
                    sb.append("\t\tput(\"").append(kvs[0]).append("\",\"").append(kvs[1]).append("\");\n");
                }
            } catch (Exception e) {
                throw new ValidationException(rec.l10n("selection格式不正确,请修改"));
            }
        }

        return sb.toString();
    }

    @Model.Constrains("selection_values")
    public void checkSelectionValues(Records rec) {
        for (Records records : rec) {
            String selectionValues = (String) records.get("selection_values");
            if (Utils.isEmpty(selectionValues)) {
                return;
            }
            String[] split = selectionValues.split("\n");
            for (String s : split) {
                if (s.split(":").length != 2) {
                    throw new ValidationException(rec.l10n("selection格式不正确!请修改"));
                }
            }
        }
    }
}
