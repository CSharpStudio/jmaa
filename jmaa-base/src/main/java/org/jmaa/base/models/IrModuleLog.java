package org.jmaa.base.models;

import org.jmaa.sdk.*;

/**
 * 模块升级日志
 *
 * @author eric
 */
@Model.Meta(name = "ir.module.log", label = "模块升级日志", logAccess = BoolState.False, authModel = "ir.module")

public class IrModuleLog extends Model {
    static Field name = Field.Char().label("名称");
    static Field content = Field.Text().label("内容");
    static Field level = Field.Selection(new Options() {{
        put("info", "信息");
        put("error", "错误");
    }}).defaultValue("info");
    static Field log_time = Field.DateTime().label("时间");
    static Field state = Field.Boolean().label("状态");
}
