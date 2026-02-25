package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.craft_order_quota", label = "制程单工时定额", authModel = "as.craft_order", order = "priority desc")
public class CraftOrderQuota extends Model {
    static Field order_id = Field.Many2one("as.craft_order").label("制程单");
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field cycle_time = Field.Integer().label("标准周期(秒/单位)").greaterThen(0).required();
    static Field priority = Field.Integer().label("优先级").help("值越大越优先").defaultValue(100).required();
}
