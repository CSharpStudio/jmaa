package jmaa.modules.md.process.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_station", label = "工位")
public class WorkStation extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field process_id = Field.Many2one("md.work_process").label("关联工序").required();
    static Field work_step_id = Field.Many2one("md.work_step").label("关联工步");
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").related("resource_id.workshop_id");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);

    @ActionMethod
    public Action workResourceChangeAction(Records record) {
        Records resource = record.getRec("resource_id");
        return Action.attr().setValue("workshop_id", resource.getRec("workshop_id"));
    }
}
