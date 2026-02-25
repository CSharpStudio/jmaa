package jmaa.modules.account.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "purchase.price", label = "采购价目", inherit = "mixin.company")
@Model.UniqueConstraint(name = "purchase_price_unique", fields = {"company_id", "type", "supplier_id"})
public class PurchasePrice extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field type = Field.Selection(new Options() {{
        put("purchase", "采购");
        put("outsource", "委外");
    }}).label("类型").required().defaultValue("purchase");
    static Field currency_id = Field.Many2one("md.currency").label("币别").required();
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field details_ids = Field.One2many("purchase.price_details", "purchase_price_id").label("明细");
}
