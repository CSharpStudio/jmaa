package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

/**
 * @author eric
 */
@Model.Meta(name = "wms.return_supplier_report", label = "退供应商查询", table = "wms_return_supplier_line", inherit = "wms.return_supplier_line")
@Model.Service(remove = "@edit")
public class ReturnSupplierReport extends Model {
    static Field supplier_id = Field.Many2one("md.supplier").related("return_id.supplier_id");
    static Field company_id = Field.Many2one("res.company").related("return_id.company_id");
}
