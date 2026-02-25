package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.sub_resource_sub_product", label = "辅助资源附带产品", inherit = "mixin.material", authModel = "md.sub_resource_product")
public class SubResourceSubProduct extends Model {
    static Field qty = Field.Integer().label("数量").greaterThen(0).defaultValue(1).required();
    static Field main_id = Field.Many2one("md.sub_resource_product").label("主产品");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
