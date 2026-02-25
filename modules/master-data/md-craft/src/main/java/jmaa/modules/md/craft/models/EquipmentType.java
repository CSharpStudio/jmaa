package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.equipment_type")
public class EquipmentType extends Model {
    static Field craft_type_id = Field.Many2one("md.craft_type").label("制程类型");
}
