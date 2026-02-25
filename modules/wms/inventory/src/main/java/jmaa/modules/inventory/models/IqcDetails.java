package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
@Model.Meta(name = "iqc.material_details", label = "送检明细",authModel = "iqc.sheet", table = "stock_stock_in_details", inherit = {"stock.stock_in_details"})
public class IqcDetails extends Model {
    static Field iqc_id = Field.Many2one("iqc.sheet").label("来料检验单");
}
