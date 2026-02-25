package jmaa.modules.md.product.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;


@Model.Meta(name = "md.product_family", label = "产品族")
public class ProductFamily extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field remark = Field.Char().label("备注");
    static Field product_ids = Field.One2many("md.material", "family_id").label("产品列表");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
