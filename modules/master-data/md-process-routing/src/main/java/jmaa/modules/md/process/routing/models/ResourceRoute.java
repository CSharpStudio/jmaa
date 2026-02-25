package jmaa.modules.md.process.routing.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "pr.resource_route", label = "产线工艺路线")
@Model.UniqueConstraint(name = "resource_date_unique", fields = {"resource_id", "begin_date"})
public class ResourceRoute extends Model {
    static Field begin_date = Field.Date().label("生效时间").required();
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间");
    static Field resource_id = Field.Many2one("md.work_resource").label("产线").required();
    static Field route_version_id = Field.Many2one("pr.route_version").label("工艺路线").required();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
}
