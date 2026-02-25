package jmaa.modules.wip.models;

import org.jmaa.sdk.Action;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

@Model.Meta(name = "mfg.product_route", label = "产品工艺路线", inherit = {"mixin.material"})
@Model.UniqueConstraint(name = "material_date_unique", fields = {"material_id", "begin_date"})
public class ProductRoute extends Model {
    static Field product_family_id = Field.Many2one("md.product_family").related("material_id.family_id");
    static Field begin_date = Field.Date().label("生效时间").required();
    static Field route_version_id = Field.Many2one("pr.route_version").label("工艺路线").required();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    /**
     * 带出物料相关信息
     */
    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"))
            .setValue("product_family_id", material.getRec("family_id").getPresent());
    }
}
