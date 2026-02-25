package jmaa.modules.stock.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "stock.stock_out_details", label = "出库明细", authModel = "stock.stock_out", inherit = {"mixin.material", "mixin.material_label"})
public class StockOutDetails extends ValueModel {
    static Field stock_out_id = Field.Many2one("stock.stock_out").label("出库单号");
    static Field qty = Field.Float().label("数量");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field status = Field.Selection(new Options() {{
        put("new", "待出库");
        put("done", "完成");
    }}).label("状态").defaultValue("new");
    static Field lot_num = Field.Char().label("物料批次号");
}
