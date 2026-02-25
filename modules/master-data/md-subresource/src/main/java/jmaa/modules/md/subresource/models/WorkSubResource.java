package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.Action;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;

@Model.Meta(name = "md.work_sub_resource", label = "主-辅助资源匹配", inherit = "md.sub_work_resource", table = "md_sub_work_resource", authModel = "md.work_resource")
public class WorkSubResource extends Model {
    static Field specification = Field.Char().related("model_id.specification");
    static Field type_id = Field.Many2one("md.sub_resource_type").related("model_id.type_id");

    @ActionMethod
    public Object onModelChange(Records record) {
        Records model = record.getRec("model_id");
        return Action.attr().setValue("specification", model.get("specification"))
            .setValue("type_id", model.getRec("type_id"));
    }
}
