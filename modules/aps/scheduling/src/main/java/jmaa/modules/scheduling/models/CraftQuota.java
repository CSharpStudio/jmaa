package jmaa.modules.scheduling.models;

import org.jmaa.sdk.Action;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

@Model.Meta(name = "as.craft_quota", label = "制程标准工时")
@Model.UniqueConstraint(name = "craft_quota_unique", fields = {"craft_process_id", "resource_id", "material_id"})
public class CraftQuota extends Model {
    static Field craft_process_id = Field.Many2one("md.craft_process").label("制程名称").required();
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源");
    static Field transfer_time = Field.Integer().label("转款时间(秒)").min(0).defaultValue(0).required();
    static Field cycle_time = Field.Integer().label("标准周期(秒/单位)").greaterThen(0).required();
    static Field priority = Field.Integer().label("优先级").help("值越大越优先").defaultValue(100).required();
    static Field material_id = Field.Many2one("md.material").label("物料");
    static Field material_name_spec = Field.Char().related("material_id.name_spec");

    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"));
    }

    @Model.ActionMethod
    public Action onProcessChange(Records record) {
        Records process = record.getRec("craft_process_id");
        return Action.attr().setValue("transfer_time", process.get("transfer_time"));
    }
}
