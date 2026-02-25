package jmaa.modules.inventory.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "oqc.material_details", label = "送检明细",authModel = "oqc.sheet", table = "stock_stock_out_details", inherit = {"stock.stock_out_details"})
public class OqcDetails extends Model {
    static Field oqc_id = Field.Many2one("oqc.sheet").label("出货检验单");
}
