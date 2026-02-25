package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "lbl.lot_status", label = "批号状态表")
public class LotStatus extends Model {
    static Field sn = Field.Char().label("序列号");
    static Field lot_num = Field.Char().label("批次号");
    static Field material_id = Field.Many2one("md.material").label("物料");
    static Field order_id = Field.Char().label("单号");
    static Field type = Field.Selection(new Options() {{
        put("wms.material_receipt", "来料接收");
        put("wms.pick_out", "挑选");
        put("wms.return_supplier", "退供应商");
        put("stock.stock_in", "入库单");
        put("wms.other_stock_in", "其他入库单");
        put("wms.other_stock_out", "其他出库单");
        put("wms.sales_return", "销售退货");
        put("wms.transfer_order", "仓库调拨");
    }}).label("单据类型").defaultValue("wms.material_receipt");
    static Field detail_id = Field.Char().label("明细行id");
}
