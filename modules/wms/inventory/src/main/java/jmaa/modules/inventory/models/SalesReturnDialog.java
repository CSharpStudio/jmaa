package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "wms.sales_return_dialog", label = "销售退货弹框", authModel = "wms.sales_return")
public class SalesReturnDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").lookup("searchWarehouse");
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id");
    static Field material_name_spec = Field.Char().related("material_id.name_spec");
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field auto_confirm = Field.Boolean().label("自动确认");
    static Field request_qty = Field.Float().label("需求数量").readonly();
    static Field return_qty = Field.Float().label("待退数量").readonly();
    static Field qty = Field.Float().label("扫码数量").readonly();
    static Field sn = Field.Char().label("条码");
    static Field message = Field.Text().label("提示信息");
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }
}
