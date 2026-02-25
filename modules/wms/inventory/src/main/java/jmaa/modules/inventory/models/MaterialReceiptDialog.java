package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "wms.material_receipt_dialog", label = "收料对话框", authModel = "wms.material_receipt")
public class MaterialReceiptDialog extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id");
    static Field material_name_spec = Field.Char().label("名称规格").related("material_id.name_spec");
    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule");
    static Field print_tpl_id = Field.Many2one("print.template").label("标签模板").required();
    static Field min_packages = Field.Float().label("标签数量").related("material_id.min_packages");
    static Field request_qty = Field.Float().label("预收数量").readonly();
    static Field deficit_qty = Field.Float().label("待收数量").readonly();
    static Field commit_qty = Field.Float().label("实收数量(不含赠品)");
    static Field gift_qty = Field.Float().label("赠品数").defaultValue(0D);
    static Field label_count = Field.Integer().label("标签张数").help("标签张数=(实收数量+赠品数)/标签数量");
    static Field product_date = Field.Date().label("生产日期").required();
    static Field product_lot = Field.Char().label("生产批次");
    static Field lot_attr = Field.Char().label("批次属性");
    static Field lpn = Field.Char().label("LPN");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("收货仓库").required().lookup("searchWarehouse");
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    static Field sn = Field.Char().label("条码");
    static Field message = Field.Text().label("提示信息");
    static Field auto_confirm = Field.Boolean().label("自动确认");

}
