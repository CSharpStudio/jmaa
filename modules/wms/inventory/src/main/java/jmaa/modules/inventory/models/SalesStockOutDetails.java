package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_stock_out_details", label = "销售出库明细", table = "stock_stock_out_details", authModel = "wms.sales_stock_out", inherit = "stock.stock_out_details")
public class SalesStockOutDetails extends Model {
    static Field sales_stock_out_id = Field.Many2one("wms.sales_stock_out").label("销售出库");
}
