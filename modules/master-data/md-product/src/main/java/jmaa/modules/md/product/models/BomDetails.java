package jmaa.modules.md.product.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.bom_details", label = "物料清单", inherit = {"mixin.material"}, authModel = "md.bom")
@Model.UniqueConstraint(name = "material_id_unique", fields = {"version_id", "material_id"})
public class BomDetails extends Model {
    static Field qty = Field.Float().label("用量").required().greaterThen(0d);
    static Field is_alternative = Field.Boolean().label("是否替代料").defaultValue(false);
    static Field main_material_id = Field.Many2one("md.material").label("主料");
    static Field version_id = Field.Many2one("md.bom_version").label("BOM版本");
}
