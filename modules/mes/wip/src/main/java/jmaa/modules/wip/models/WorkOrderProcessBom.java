package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "mfg.work_order_process_bom", label = "工单工序BOM", authModel = "mfg.work_order", inherit = {"mfg.work_order_bom"})
@Model.UniqueConstraint(name = "material_process_unique", fields = {"work_order_id", "process_id", "material_id"}, message = "工序BOM重复")
public class WorkOrderProcessBom extends Model {
    static Field process_id = Field.Many2one("md.work_process").label("工序").required();
    static Field key_module = Field.Boolean().label("关键组件");
    static Field qty = Field.Float().label("工序用量");
    static Field require_qty = Field.Float().store(false);
}
