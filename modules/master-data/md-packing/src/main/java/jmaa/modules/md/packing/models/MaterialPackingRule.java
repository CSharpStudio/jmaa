package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.material_packing_rule", label = "物料包装规则", inherit = "mixin.material", authModel = "md.material")
@Model.UniqueConstraint(name = "name_material_unique", fields = {"name", "material_id"})
public class MaterialPackingRule extends Model {
    static Field name = Field.Char().label("规则名称").required();
    static Field material_id = Field.Many2one("md.material").label("产品");
    static Field rule_id = Field.Many2one("md.packing_rule").label("规则");
    static Field is_default = Field.Boolean().label("是否默认");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field description = Field.Char().label("规则描述");

    @ServiceMethod(label = "设置默认")
    public Action setDefault(Records record) {
        record.ensureOne();
        record.getEnv().getCursor().execute("update md_material_packing_rule set is_default=%s where material_id=%s",
            Utils.asList(false, record.getRec("material_id").getId()));
        record.set("is_default", true);
        return Action.success();
    }
}
