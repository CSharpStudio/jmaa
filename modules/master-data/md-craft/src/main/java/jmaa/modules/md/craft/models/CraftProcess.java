package jmaa.modules.md.craft.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Selection;

@Model.Meta(name = "md.craft_process", label = "制程工艺")
public class CraftProcess extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field craft_type_id = Field.Many2one("md.craft_type").label("制程类型");
    static Field craft_section = Field.Selection(Selection.related("md.factory", "craft_section")).label("工段");
    static Field transfer_time = Field.Integer().label("转款时间(秒)").min(0);
    static Field cycle_time = Field.Integer().label("标准周期(秒/单位)").greaterThen(0);
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
