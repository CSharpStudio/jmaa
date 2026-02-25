package jmaa.modules.bbs.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "bbs.follower", label = "关注者", logAccess = BoolState.False)
public class Follower extends Model {
    static Field res_model = Field.Char().label("关注的模型名称").index().required();
    static Field res_id = Field.Many2oneReference("res_model").label("模型").index();
    static Field user_id = Field.Many2one("rbac.user").label("关注者").index().ondelete(DeleteMode.Cascade).required();
    static Field name = Field.Char().label("账号名称").related("user_id.present");
    static Field image = Field.Image().related("user_id.image");
    static Field active = Field.Boolean().related("user_id.active");
}
