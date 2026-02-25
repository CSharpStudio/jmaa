package jmaa.modules.md.product.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.product_model", label = "产品机型")
public class ProductModel extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field material_ids = Field.One2many("md.material", "model_id").label("产品");
}
