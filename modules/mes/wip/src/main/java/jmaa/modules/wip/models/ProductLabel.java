package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "lbl.product_label")
public class ProductLabel extends Model {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
}
