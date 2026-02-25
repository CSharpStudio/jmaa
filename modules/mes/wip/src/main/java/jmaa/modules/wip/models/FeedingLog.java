package jmaa.modules.wip.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Service(remove = "@edit")
@Model.Meta(name = "mfg.feeding_log", label = "上/下料记录", inherit = {"mixin.material", "mixin.material_label", "mixin.company"}, logAccess = BoolState.False)
public class FeedingLog extends Model {
    static Field workshop_id = Field.Many2one("md.workshop").related("station_id.resource_id.workshop_id");
    static Field resource_id = Field.Many2one("md.work_resource").related("station_id.resource_id");
    static Field process_id = Field.Many2one("md.work_process").related("station_id.process_id");
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
    static Field qty = Field.Float().label("数量");
    static Field create_uid = Field.Many2one("rbac.user").label("操作人");
    static Field create_date = Field.DateTime().label("操作时间");
    static Field lot_num = Field.Char().label("物料批次号");
}
