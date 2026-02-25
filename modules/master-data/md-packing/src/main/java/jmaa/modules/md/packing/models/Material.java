package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(inherit = {"md.material"})
public class Material extends Model {
    static Field packing_rule_ids = Field.One2many("md.material_packing_rule","material_id").label("包装规则");
}
