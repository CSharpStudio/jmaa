package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

@Model.Meta(name = "mfg.feeding_dialog", label = "上料对话框", inherit = "mixin.material")
public class FeedingDialog extends ValueModel {
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").required();
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field process_id = Field.Many2one("md.work_process").label("工序").required();
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单").required();

    @ActionMethod
    public Action onStationChange(Records record){
        Records station = record.getRec("station_id");
        AttrAction action = new AttrAction();
        action.setValue("process_id", station.getRec("process_id"));
        Records resource = station.getRec("resource_id");
        action.setValue("resource_id", resource);
        action.setValue("workshop_id", resource.getRec("workshop_id"));
        return action;
    }
}
