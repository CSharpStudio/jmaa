package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_return_report", label = "退供应商明细", table = "wms_sales_return_line", inherit = "wms.sales_return_line")
@Model.Service(remove = "@edit")
public class SalesReturnReport extends Model {
    static Field customer_id = Field.Many2one("md.customer").related("return_id.customer_id");
    static Field company_id = Field.Many2one("res.company").related("return_id.company_id");
}
