package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(name = "as.scheduler", label = "排产员")
public class Scheduler extends Model {
    static Field user_id = Field.Many2one("rbac.user").label("用户").unique();
    static Field name = Field.Char().related("user_id.name").label("名称");
    static Field login = Field.Char().related("user_id.login").label("账号");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field permission_ids = Field.One2many("as.scheduler_permission", "scheduler_id").label("资源权限");

    @ServiceMethod(label = "加载权限")
    public Object loadPermission(Records record) {
        Map<String, Object> result = new HashMap<>();
        Records resource = record.getEnv().get("md.work_resource").find(Criteria.equal("active", true), 0, 0, "seq,code");
        result.put("resource", resource.read(Utils.asList("present", "workshop_id", "type")));
        Records permission = record.getEnv().get("as.scheduler_permission").find(Criteria.equal("scheduler_id", record.getId()));
        result.put("permission", permission.read(Utils.asList("resource_id", "permission")));
        return result;
    }
}
