package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wms.other_stock_in_dialog", label = "其它入库弹框", authModel = "wms.other_stock_in")
public class OtherStockInDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").lookup("searchWarehouse");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id");
    static Field material_name_spec = Field.Char().related("material_id.name_spec");
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field auto_confirm = Field.Boolean().label("自动确认");
    static Field request_qty = Field.Float().label("需求数量").readonly();
    static Field scan_qty = Field.Float().label("已扫数量").readonly();
    static Field qty = Field.Float().label("扫码数量").readonly();
    static Field sn = Field.Char().label("条码");
    static Field message = Field.Text().label("提示信息");
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }
}
