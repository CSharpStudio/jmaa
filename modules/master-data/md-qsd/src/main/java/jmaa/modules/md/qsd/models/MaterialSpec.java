package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.tools.ArrayUtils;

import java.util.List;
import java.util.Map;

@Model.Meta(name = "qsd.material_spec", label = "料号检验标准", present = {"code", "version"}, presentFormat = "{code}({version})", inherit = "mixin.material", order = "version desc")
public class MaterialSpec extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field version = Field.Char().label("版本").required();
    static Field type = Field.Selection(Selection.related("qsd.quality_class_spec", "type")).label("检验类型").required();
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");
    static Field item_ids = Field.One2many("qsd.material_spec_item", "spec_id").label("检验项目");

    public List<String> getUniqueFields(Records records) {
        return Utils.asList("material_id", "version", "type");
    }

    public void validateUnique(Records records, Map<String, Object> values, boolean create) {
        org.jmaa.sdk.core.UniqueConstraint constraint = new org.jmaa.sdk.core.UniqueConstraint("class_unique",
            getUniqueFields(records).toArray(ArrayUtils.EMPTY_STRING_ARRAY), "", false, "qsd");
        boolean match = false;
        for (String field : constraint.getFields()) {
            if (values.containsKey(field)) {
                match = true;
                break;
            }
        }
        if (match) {
            if (create) {
                validateUniqueConstraint(records, constraint, values, create);
            } else {
                for (Records rec : records) {
                    validateUniqueConstraint(rec, constraint, values, create);
                }
            }
        }
        callSuper(records, values, create);
    }
}
