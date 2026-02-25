package org.jmaa.base.models;

import org.jmaa.sdk.*;

/**
 * 用户日志
 *
 * @author Eric Liang
 */

@Model.Meta(name = "rbac.user.log", label = "用户日志", authModel = "rbac.user", logAccess = BoolState.False)
public class RbacUserLog extends Model {
    static Field user_id = Field.Many2one("rbac.user").label("用户");
    static Field login = Field.Char().label("账号");
    static Field log_time = Field.DateTime().label("登录时间");
    static Field ip = Field.Char().label("登录IP");
    static Field user_agent = Field.Char().label("UserAgent").length(1000);
    static Field result = Field.Selection(new Options() {{
        put("1", "成功");
        put("0", "失败");
        put("2", "登出");
    }}).label("结果");
}
