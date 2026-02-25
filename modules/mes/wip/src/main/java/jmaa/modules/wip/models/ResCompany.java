package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(inherit = "res.company")
public class ResCompany extends Model {
    static Field supplier_ids = Field.Many2many("md.supplier", "res_company_supplier", "company_id", "supplier_id")
        .label("生产商").help("用于打印生产标签，需要把当前公司维护到供应商信息");
}
