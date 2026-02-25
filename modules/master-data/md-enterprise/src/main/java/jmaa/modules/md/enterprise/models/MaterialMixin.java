package jmaa.modules.md.enterprise.models;

import org.jmaa.sdk.*;

/**
 * @author eric
 */
@Model.Meta(name = "mixin.material", label = "物料插件")
public class MaterialMixin extends AbstractModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码").required();
    static Field material_name_spec = Field.Char().related("material_id.name_spec");
    static Field material_category = Field.Selection().label("存货类别").related("material_id.category");
    static Field unit_id = Field.Many2one("md.unit").label("库存单位").related("material_id.unit_id");
    static Field unit_accuracy = Field.Integer().label("单位精度").related("material_id.unit_id.accuracy");

    /**
     * 带出物料相关信息
     */
    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        Records unit = material.getRec("unit_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"))
            .setValue("unit_id", unit.getPresent())
            .setValue("unit_accuracy", unit.get("accuracy"));
    }
}
