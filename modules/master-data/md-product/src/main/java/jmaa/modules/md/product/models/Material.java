package jmaa.modules.md.product.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
@Model.Meta(inherit = "md.material")
public class Material extends Model {
    static Field family_id = Field.Many2one("md.product_family").label("产品族");
    static Field model_id = Field.Many2one("md.product_model").label("产品机型");
}
