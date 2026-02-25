package jmaa.modules.stock.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "stock.stock_in", label = "入库单", inherit = {"code.auto_code", "mixin.company"})
public class StockIn extends ValueModel {
    static Field code = Field.Char().label("单号").unique();
    static Field type = Field.Selection(new Options() {{
        put("purchase", "采购入库");
        put("wms.sales_return", "销售退货入库");
        put("wms.transfer_order", "调拨入库");
        put("mfg.material_return", "生产退料入库");
        put("mfg.product_storage_notice", "成品入库通知");
        put("wms.inventory_balance", "盘盈");
        put("wms.other_stock_in", "其它入库");
    }}).label("入库类型");
    static Field related_code = Field.Char().label("相关单据");
    static Field related_model = Field.Char().label("相关单据模型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据ID");
    static Field status = Field.Selection(new Options() {{
        put("new", "待入库");
        put("to-inspect", "待检验");
        put("stocking", "入库中");
        put("done", "完成");
    }}).label("单据状态").defaultValue("new");
    static Field remark = Field.Char().label("备注");
}
