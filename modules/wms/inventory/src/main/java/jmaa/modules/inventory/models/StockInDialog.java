package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "stock.stock_in_dialog",label = "入库单扫码弹框")
public class StockInDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").readonly();
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule").readonly();
    static Field auto_confirm = Field.Boolean().label("自动确认");
}
