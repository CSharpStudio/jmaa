package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "mfg.material_issue_details", label = "发料明细", table = "stock_stock_out_details", authModel = "mfg.material_issue", inherit = "stock.stock_out_details")
public class MaterialIssueDetails extends Model {
    static Field issue_id = Field.Many2one("mfg.material_issue").label("发料单");
}
