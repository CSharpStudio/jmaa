package jmaa.modules.md.subresource.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(inherit = "md.work_resource")
public class WorkResource extends Model {
    static Field sub_resource_ids = Field.One2many("md.work_sub_resource", "work_resource_id").label("制造资源");
}
