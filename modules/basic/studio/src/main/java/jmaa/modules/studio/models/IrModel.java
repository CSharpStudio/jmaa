package jmaa.modules.studio.models;

import org.jmaa.sdk.*;

@Model.Meta(inherit = "ir.model")
public class IrModel extends Model {
    @Model.ServiceMethod(label = "是否有设计权限", auth = "read", ids = false)
    public boolean canDesign(Records record) {
        Records security = record.getEnv().get("rbac.security");
        return (boolean) security.call("hasPermission", record.getEnv().getUserId(), "dev.studio", "design");
    }
}
