package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "md.unit", present = {"name", "symbol"}, presentFormat = "{name}({symbol})", label = "单位")
public class Unit extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field symbol = Field.Char().label("单位符号");
    static Field type = Field.Selection(new Options() {{
        put("length", "长度单位");
        put("weight", "重量单位");
        put("quantity", "数量单位");
        put("volume", "容量单位");
    }}).label("单位类型");

    static Field accuracy = Field.Integer().label("小数位").defaultValue(0);

    static Field description = Field.Char().label("描述").help("单位的描述").length(255);

    static Field is_base = Field.Boolean().label("是否主单位").required().defaultValue(false);

    static Field derived_ids = Field.One2many("md.unit_conversion", "base_id").label("单位转换");

    static Field base_ids = Field.One2many("md.unit_conversion", "derived_id").label("主单位转换");

    static Field base_conversion = Field.Object().label("与主单位比例").compute("getBaseConversion", "setBaseConversion");

    public Object getBaseConversion(Records record) {
        if (record.getBoolean("is_base")) {
            return Utils.asList(1, record.getId(), record.get("present"));
        } else {
            Map<String, Object> ctx = record.getEnv().getContext();
            Records conversions = (Records) ctx.computeIfAbsent("unit-conversion",
                k -> record.getEnv().get("md.unit_conversion").find(new Criteria()));
            Records base = conversions.filter(c -> Utils.equals(c.getRec("base_id").get("type"), record.get("type"))
                && record.equals(c.getRec("derived_id")));
            if (base.any()) {
                base = base.first();
                return Utils.asList(base.getDouble("ratio"), base.getRec("base_id").getId(),
                    base.getRec("base_id").getString("present"));
            }
        }
        return Collections.emptyList();
    }

    public void setBaseConversion(Records record) {
        List<Object> values = (List) record.get("base_conversion");
        if (Utils.isNotEmpty(values)) {
            double ratio = Utils.toDouble(values.get(0));
            String baseId = Utils.toString(values.get(1));
            Records conversion = record.getEnv().get("md.unit_conversion").find(Criteria.equal("base_id", baseId).and("derived_id", "=", record.getId()));
            if (conversion.any()) {
                conversion.set("ratio", ratio);
            } else {
                conversion.create(new KvMap() {{
                    put("base_id", baseId);
                    put("derived_id", record.getId());
                    put("ratio", ratio);
                }});
            }
        }
    }

    @Constrains("is_base")
    public void checkIsBase(Records records) {
        for (Records record : records) {
            boolean isBase = record.getBoolean("is_base");
            if (isBase) {
                String type = record.getString("type");
                Records exists = records.find(Criteria.equal("type", type).and("is_base", "=", true).and("id", "!=", record.getId()));
                if (exists.any()) {
                    throw new ValidationException(records.l10n("单位类型[%s]，已存在主单位[%s]", record.getSelection("type"), exists.get("present")));
                }
            } else if (record.getRec("derived_ids").any()) {
                throw new ValidationException(records.l10n("[%s]存在单位转换，不能取消主单位", record.get("present")));
            }
        }
    }
}
