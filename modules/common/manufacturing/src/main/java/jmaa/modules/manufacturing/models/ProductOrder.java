package jmaa.modules.manufacturing.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "mfg.product_order", label = "生产订单", inherit = {"code.auto_code", "mixin.company", "mixin.material", "mixin.order_status"})
public class ProductOrder extends Model {
    static Field code = Field.Char().label("生产订单号").unique();
    static Field plan_qty = Field.Float().label("计划数量").required().greaterThen(0d).tracking();
    static Field factory_id = Field.Many2one("md.factory").label("指定工厂").tracking();
    static Field customer_due_date = Field.Date().label("客户交期").required().tracking();
    static Field factory_due_date = Field.Date().label("工厂交期").required().tracking();
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field sales_order_id = Field.Many2one("sales.order").label("销售订单");
    static Field remark = Field.Char().label("备注");
}
