package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.craft_route_bom", label = "产品制程路线物料", authModel = "md.craft_route_product")
@Model.UniqueConstraint(name = "node_unique", fields = {"product_id", "node_id"})
public class CraftRouteBom extends Model {
    static Field product_id = Field.Many2one("md.craft_route_product").label("产品制程路线").ondelete(DeleteMode.Cascade);
    static Field node_id = Field.Many2one("md.craft_route_node").label("制程路线").ondelete(DeleteMode.Cascade);
    static Field material_id = Field.Many2one("md.material").label("物料编码").ondelete(DeleteMode.Cascade);
    static Field material_name_spec = Field.Char().related("material_id.name_spec").label("规格名称");
    static Field material_category = Field.Selection().related("material_id.category").label("物料分类");

    @ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"));
    }
}
