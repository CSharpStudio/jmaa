package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.scheduler_permission", label = "排产权限", authModel = "as.scheduler")
public class SchedulerPermission extends Model {
    static Field scheduler_id = Field.Many2one("as.scheduler").label("排产员");
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源");
    static Field resource_code = Field.Char().related("resource_id.code");
    static Field resource_name = Field.Char().related("resource_id.name");
    static Field resource_type = Field.Selection().related("resource_id.type");
    static Field workshop_id = Field.Many2one("md.workshop").related("resource_id.workshop_id");
    static Field permission = Field.Selection(new Options() {{
        put("read", "查看");
        put("edit", "编辑");
    }}).label("权限");
}
