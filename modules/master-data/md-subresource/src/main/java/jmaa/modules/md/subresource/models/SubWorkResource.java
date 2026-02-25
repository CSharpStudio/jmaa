package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.sub_work_resource", label = "主-辅助资源匹配", order = "priority desc", authModel = "md.sub_resource_model")
@Model.UniqueConstraint(name = "ar_work_resource_unique", fields = {"work_resource_id", "model_id"})
public class SubWorkResource extends Model {
    static Field priority = Field.Integer().label("优先级").required().defaultValue(100).help("数值越大，优先级越高");
    static Field work_resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field type = Field.Selection().related("work_resource_id.type");
    static Field workshop_id = Field.Many2one("md.workshop").related("work_resource_id.workshop_id");
    static Field model_id = Field.Many2one("md.sub_resource_model").label("辅助资源型号").required();
    static Field craft_type_id = Field.Many2one("md.craft_type").related("work_resource_id.craft_type_id");

    @ActionMethod
    public Object onResourceChange(Records record) {
        Records resource = record.getRec("work_resource_id");
        return Action.attr().setValue("type", resource.get("type"))
            .setValue("workshop_id", resource.getRec("workshop_id"));
    }
}
