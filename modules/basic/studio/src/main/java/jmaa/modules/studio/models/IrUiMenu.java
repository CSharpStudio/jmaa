package jmaa.modules.studio.models;

import org.jmaa.sdk.Action;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.exceptions.AccessException;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.SecurityCode;

import java.util.List;

@Model.Meta(inherit = "ir.ui.menu")
public class IrUiMenu extends Model {
    @Model.ServiceMethod(label = "是否有设计权限", auth = "read", ids = false)
    public boolean canDesign(Records record) {
        Records security = record.getEnv().get("rbac.security");
        return (boolean) security.call("hasPermission", record.getEnv().getUserId(), "dev.studio", "design");
    }

    @Model.ServiceMethod(auth = "read", ids = false, label = "创建模型")
    public Object createModel(Records record, String parentMenu, String name, String model, List<String> features) {
        if (!canDesign(record)) {
            throw new AccessException("没有权限，请联系管理员分配权限", SecurityCode.NO_PERMISSION);
        }
        record.getEnv().get("dev.studio").call("createModel", name, model, features);
        record.create(new KvMap()
            .set("name", name)
            .set("model", model)
            .set("view", "grid,form")
            .set("parent_id", parentMenu));
        return Action.success();
    }
}
