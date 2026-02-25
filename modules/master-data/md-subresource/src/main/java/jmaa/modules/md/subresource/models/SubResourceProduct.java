package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.sub_resource_product", label = "辅助资源产品", inherit = "mixin.material", authModel = "md.sub_resource")
@Model.UniqueConstraint(name = "ar_material_resource_unique", fields = {"material_id", "sub_resource_id"})
public class SubResourceProduct extends Model {
    static Field qty = Field.Integer().label("数量").greaterThen(0).defaultValue(1).required();
    static Field multi_product = Field.Boolean().label("是否附带产品");
    static Field cycle_time = Field.Float().label("标准周期(秒/单位)").required().greaterThen(0d);
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field sub_ids = Field.One2many("md.sub_resource_sub_product", "main_id").label("附带产品");
    static Field sub_resource_id = Field.Many2one("md.sub_resource").label("辅助资源").required();
}
