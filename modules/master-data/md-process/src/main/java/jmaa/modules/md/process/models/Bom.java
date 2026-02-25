package jmaa.modules.md.process.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(inherit = "md.bom")
public class Bom extends Model {
    static Field process_bom_ids = Field.One2many("md.bom_process","bom_id").label("工序BOM");
}
