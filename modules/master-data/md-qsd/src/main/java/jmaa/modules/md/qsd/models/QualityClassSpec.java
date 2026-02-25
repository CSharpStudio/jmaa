package jmaa.modules.md.qsd.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.UniqueConstraint;
import org.jmaa.sdk.tools.ArrayUtils;

import java.util.List;
import java.util.Map;

@Model.Meta(name = "qsd.quality_class_spec", label = "分类检验标准", order = "version desc", present = {"code", "version"}, presentFormat = "{code}({version})")
//@Model.UniqueConstraint(name = "class_type_version_unique", fields = {"class_id", "version", "type"})
public class QualityClassSpec extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field class_id = Field.Many2one("qsd.quality_class").label("质量分类").required();
    static Field version = Field.Char().label("版本").required();
    static Field type = Field.Selection().label("检验类型").required();
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().defaultValue(true).label("是否有效");
    static Field item_ids = Field.One2many("qsd.quality_class_spec_item", "spec_id").label("检验项目");

    public List<String> getUniqueFields(Records records) {
        return Utils.asList("class_id", "version", "type");
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
