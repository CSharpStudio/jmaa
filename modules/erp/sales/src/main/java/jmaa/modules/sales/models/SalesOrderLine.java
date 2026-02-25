package jmaa.modules.sales.models;

import org.jmaa.sdk.*;
@Model.Meta(name = "sales.order_line", label = "销售订单明细", order = "line_no", authModel = "sales.order", inherit = "mixin.material")
public class SalesOrderLine extends Model {
    static Field so_id = Field.Many2one("sales.order").label("销售订单");
    static Field line_no = Field.Integer().label("行号").required();
    static Field sales_qty = Field.Float().label("销售数量").required();
    static Field status = Field.Selection(new Options() {{
        put("new", "新建");
        put("deliver", "已发货");
        put("done", "已完成");
        put("close", "关闭");
    }}).label("状态").defaultValue("new");
    static Field deliver_qty = Field.Float().label("发货数量");
    static Field return_qty = Field.Float().label("退货数量");
}
