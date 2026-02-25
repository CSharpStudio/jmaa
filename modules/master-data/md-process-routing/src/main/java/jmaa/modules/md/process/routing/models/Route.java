package jmaa.modules.md.process.routing.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "pr.route", label = "工艺路线", inherit = "mixin.company")
public class Route extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field family_id = Field.Many2one("md.product_family").label("产品族").required();
    static Field version_ids = Field.One2many("pr.route_version", "route_id").label("工艺路线版本");
    static Field remark = Field.Char().label("备注");
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
