package jmaa.modules.md.process.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.bom_process", label = "工序BOM", inherit = {"mixin.material"}, authModel = "md.bom")
@Model.UniqueConstraint(name = "material_id_unique", fields = {"bom_id", "process_id", "material_id"}, message = "BOM工序物料清单重复")
public class BomProcess extends Model {
    static Field process_id = Field.Many2one("md.work_process").label("工序").required();
    static Field qty = Field.Float().label("用量").required().greaterThen(0d);
    static Field is_alternative = Field.Boolean().label("是否替代料").defaultValue(false);
    static Field main_material_id = Field.Many2one("md.material").label("主料");
    static Field bom_id = Field.Many2one("md.bom").label("产品").required();
    static Field key_module = Field.Boolean().label("关键组件");
}
