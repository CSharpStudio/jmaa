package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.craft_route_product", label = "产品制程路线")
@Model.UniqueConstraint(name = "material_route_unique", fields = {"material_id", "craft_route_id"})
public class CraftRouteProduct extends Model {
    static Field material_id = Field.Many2one("md.material").label("产品编码").ondelete(DeleteMode.Cascade);
    static Field material_name_spec = Field.Char().related("material_id.name_spec").label("规格名称");
    static Field material_category = Field.Selection().related("material_id.category").label("物料分类");
    static Field craft_route_id = Field.Many2one("md.craft_route").label("制程路线").ondelete(DeleteMode.Cascade);
    static Field is_default = Field.Boolean().label("是否默认");
    static Field bom_ids = Field.One2many("md.craft_route_bom", "product_id").label("物料清单");

    @ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"));
    }

    @ServiceMethod(label = "设置默认")
    public Action setDefault(Records record) {
        record.ensureOne();
        record.find(Criteria.equal("material_id", record.getRec("material_id").getId())).set("is_default", false);
        record.set("is_default", true);
        return Action.reload(record.l10n("操作成功"));
    }
}
