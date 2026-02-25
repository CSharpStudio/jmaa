package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "mfg.product_order_bom", label = "生产订单BOM", inherit = {"mixin.material"}, authModel = "mfg.product_order")
public class ProductOrderBom extends Model {
    static Field qty = Field.Float().label("用量").required().greaterThen(0d);
    static Field is_alternative = Field.Boolean().label("是否替代料").defaultValue(false);
    static Field main_material_id = Field.Many2one("md.material").label("主料");
    static Field order_id = Field.Many2one("mfg.product_order").label("生产订单");
}
