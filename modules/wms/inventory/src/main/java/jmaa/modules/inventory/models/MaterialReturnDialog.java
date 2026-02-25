package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "mfg.material_return_dialog")
public class MaterialReturnDialog extends ValueModel {
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").lookup("searchWarehouse");
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly();
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field print_flag = Field.Boolean().label("打印标签").defaultValue(false);
    static Field template_id = Field.Many2one("print.template").label("标签模板");
    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }
}
