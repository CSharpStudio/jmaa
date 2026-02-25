package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.warehouse")
public class Warehouse extends Model {
    static Field stock_out_rule = Field.Selection(new Options() {{
        put("fifo", "先进先出");
        put("lifo", "后进先出");
    }}).label("出库规则");
    static Field stock_out_accuracy = Field.Integer().label("FIFO/LIFO管控精度(天)").min(1);
    static Field bulk_fo = Field.Boolean().label("散料先出");
    static Field transfer_receipt = Field.Boolean().label("调拨接收").help("开启为需要扫码接收入库").defaultValue(false);
}
