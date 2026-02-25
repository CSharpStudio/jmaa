package jmaa.modules.purchase.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field supplier_material_ids = Field.One2many("purchase.supplier_material", "material_id").label("货源");
}
