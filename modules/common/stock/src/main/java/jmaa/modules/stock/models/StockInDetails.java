package jmaa.modules.stock.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "stock.stock_in_details", label = "入库明细", authModel = "stock.stock_in", inherit = {"mixin.material", "mixin.material_label"})
public class StockInDetails extends ValueModel {
    static Field status = Field.Selection(new Options() {{
        put("new", "新建"); // 来料接收,未送检状态
        put("receive", "已收货");
        put("to-inspect", "待检验");
        put("inspect-ng", "已检验");
        put("mrb", "MRB");
        put("pick-out", "挑选");
        put("to-stock", "待入库");
        put("return", "退货");
        put("done", "完成");
    }}).label("状态").defaultValue("new");
    static Field qty = Field.Float().label("数量");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field onhand_datetime = Field.DateTime().label("入库时间");
    static Field lot_num = Field.Char().label("物料批次号");
    static Field return_qty = Field.Float().label("退货数量").defaultValue(0d);
    static Field material_stock_rule = Field.Selection().related("material_id.stock_rule").label("库存规则");
}
