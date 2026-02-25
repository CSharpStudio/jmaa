package jmaa.modules.wip.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "mfg.work_order_bom", label = "工单BOM", authModel = "mfg.work_order", inherit = {"mixin.material"})
@Model.UniqueConstraint(name = "material_unique", fields = {"work_order_id", "material_id"})
public class WorkOrderBom extends Model {
    static Field qty = Field.Float().label("单位用量").required().min(0d);
    static Field is_alternative = Field.Boolean().label("是否替代料").defaultValue(false);
    static Field main_material_id = Field.Many2one("md.material").label("主料");
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单").required().ondelete(DeleteMode.Cascade);
    static Field bom_qty = Field.Float().label("计划用量").help("BOM单位用量*工单计划数量").compute("computeBomQty");
    static Field require_qty = Field.Float().required().label("需求数量");

    public double computeBomQty(Records record) {
        Records wo = record.getRec("work_order_id");
        return Utils.round(wo.getDouble("plan_qty") * record.getDouble("qty"));
    }
}
