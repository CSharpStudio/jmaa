package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(inherit = "stock.stock_out_details")
public class StockOutDetails extends Model {
    static Field inventory_balance_id = Field.Many2one("wms.inventory_balance").label("盘点平账单");
}
