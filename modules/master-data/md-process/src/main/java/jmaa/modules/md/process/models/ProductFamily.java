package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;


@Model.Meta(inherit = "md.product_family")
public class ProductFamily extends Model {
    static Field process_ids = Field.One2many("md.work_process", "product_family_id").label("工序");
}
