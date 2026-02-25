package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "wms.return_supplier_dialog", label = "收料对话框", authModel = "wms.return_supplier")
public class ReturnSupplierDialog extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id");
    static Field material_name_spec = Field.Char().label("名称规格").related("material_id.name_spec");
    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule").help("数量管控物料，输入数量发料。序列号管控物料，通过扫描条码进行发料");
    static Field request_qty = Field.Float().label("需退数量").readonly();
    static Field deficit_qty = Field.Float().label("待退数量").readonly();
    static Field commit_qty = Field.Float().label("退货数量");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发料仓库").lookup("searchWarehouse");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }
    static Field sn = Field.Char().label("条码");
    static Field qty = Field.Float().label("标签数量").readonly();
    static Field message = Field.Text().label("提示信息");
}
