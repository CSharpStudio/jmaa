package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.scheme", label = "排产方案", authModel = "as.task_scheduling")
public class Scheme extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field remark = Field.Char().label("备注");
    static Field details_ids = Field.One2many("as.scheme_details", "scheme_id").label("明细");
}
