package jmaa.modules.md.resource.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_resource", label = "制造资源", order = "seq")
public class WorkResource extends Model {
    static Field code = Field.Char().label("资源编码").required().unique();
    static Field name = Field.Char().label("资源名称").required();
    static Field type = Field.Selection(new Options() {{
        put("line", "产线");
        put("equipment", "设备");
    }}).label("资源类型").required().defaultValue("line");
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field equipment_id = Field.Many2one("md.equipment").label("设备").unique();
    static Field work_type = Field.Selection(new Options() {{
        put("move", "工序采集");
        put("task", "任务报工");
    }}).label("生产方式");
    static Field seq = Field.Integer().label("显示顺序").defaultValue(10).required();
}
