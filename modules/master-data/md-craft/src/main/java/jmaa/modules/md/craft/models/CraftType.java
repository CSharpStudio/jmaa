package jmaa.modules.md.craft.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.craft_type", label = "制程类型")
public class CraftType extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field algorithm = Field.Selection(new Options() {{
        put("base", "基础");
        put("molding", "注塑");
    }}).label("排产算法");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
