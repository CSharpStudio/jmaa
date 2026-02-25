package jmaa.modules.inventory.models;


import org.jmaa.sdk.*;

@Model.Meta(name = "wms.other_stock_in_line", label = "其它入库-物料明细", authModel = "wms.other_stock_in", inherit = "mixin.material")
public class OtherStockInLine  extends Model {
    static Field other_stock_in_id = Field.Many2one("wms.other_stock_in").label("其它入库单");
    static Field status = Field.Selection(new Options(){{
        put("new", "新建");
        put("stocking", "入库中");
        put("stocked", "已备齐");
        put("done", "已完成");
    }}).label("行状态").readonly(true).defaultValue("new");
    static Field request_qty = Field.Float().label("需入库数量").required().min(0d).defaultValue(0d);
    static Field scan_qty = Field.Float().label("已扫码数量").defaultValue(0);
}
