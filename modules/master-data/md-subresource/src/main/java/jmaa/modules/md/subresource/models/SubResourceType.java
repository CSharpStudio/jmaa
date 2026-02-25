package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.sub_resource_type", label = "辅助资源类型")
public class SubResourceType extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field category = Field.Selection(new Options() {{
        put("mold", "模具");
        put("cutting", "刀具");
        put("fixture", "夹具");
        put("gauge", "检具");
        put("jig", "治具");
    }}).label("类别").required();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
