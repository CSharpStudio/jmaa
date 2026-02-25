package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_delivery_line", label = "销售发货明细", order = "line_no,id", authModel = "wms.sales_delivery", inherit = "mixin.material")
public class SalesDeliveryLine extends Model {
    static Field delivery_id = Field.Many2one("wms.sales_delivery").label("销售发货");
    static Field line_no = Field.Integer().label("行号").required();
    static Field request_qty = Field.Float().label("预发数量").required().defaultValue(0).min(0d);
    static Field delivered_qty = Field.Float().label("实发数量").required().defaultValue(0);
    static Field status = Field.Selection(new Options() {{
        put("new", "未发货");
        put("delivering", "发货中");
        put("delivered", "已备齐");
        put("done", "已完成");
    }}).label("状态").defaultValue("new");
    static Field so_line_id = Field.Many2one("sales.order_line").label("销售订单明细");
    static Field so_line_no = Field.Integer().label("订单行号").related("so_line_id.line_no");
}
