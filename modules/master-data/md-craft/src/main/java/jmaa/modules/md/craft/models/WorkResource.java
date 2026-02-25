package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.work_resource")
public class WorkResource extends Model {
    static Field craft_type_id = Field.Many2one("md.craft_type").label("制程类型");
}
