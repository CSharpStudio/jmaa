package jmaa.modules.stock.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "stock.stock_out", label = "出库单", inherit = {"code.auto_code", "mixin.company"})
public class StockOut extends ValueModel {
    static Field code = Field.Char().label("单号").unique();
    static Field type = Field.Selection(new Options() {{
        put("issue", "发料出库");
        put("sales-out", "销售出库");
        put("return-supplier", "退供应商");
        put("transfer", "仓库调拨");
        put("wms.inventory_balance", "盘亏");
        put("wms.other_stock_out", "其它出库");
    }}).label("出库类型");
    static Field related_code = Field.Char().label("相关单据");
    static Field related_model = Field.Char().label("相关单据模型");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据ID");
    static Field status = Field.Selection(new Options(){{
        put("new", "待出库");
        put("stocking", "出库中");
        put("done", "完成");
    }}).label("单据状态").defaultValue("new");
    static Field details_ids = Field.One2many("stock.stock_out_details", "stock_out_id").label("出库明细");
    static Field remark = Field.Char().label("备注");
}
