package jmaa.modules.wip.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.ValueModel;

@Model.Meta(inherit = "res.config")
public class ResConfig extends ValueModel {
    static Field feed_qty_modifiable = Field.Boolean().label("上料数量允许修改").defaultValue(false);
    static Field check_issue_material = Field.Boolean().label("退料校验发料信息").defaultValue(false);
}
