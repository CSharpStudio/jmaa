package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

import java.util.stream.Collectors;

@Model.Meta(name = "mfg.work_station_on_duty_dialog", label = "上岗")
public class WorkStationOnDutyDialog extends ValueModel {
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").required();
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").required();
    static Field process_id = Field.Many2one("md.work_process").label("工序").required();
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field staff_id = Field.Many2one("md.staff").label("员工").required();
    static Field on_duty_ids = Field.One2many("mfg.work_station_on_duty", "").label("在岗员工");
    static Field on_duty = Field.Char().label("在岗员工");

    @ActionMethod
    public Object onStationChange(Records record) {
        AttrAction action = new AttrAction();
        Records station = record.getRec("station_id");
        action.setValue("process_id", station.getRec("process_id"));
        Records resource = station.getRec("resource_id");
        action.setValue("resource_id", resource);
        action.setValue("workshop_id", resource.getRec("workshop_id"));
        action.setValue("on_duty_ids", "");
        Records onDuty = record.getEnv().get("mfg.work_station_on_duty").find(Criteria.equal("station_id", station.getId()));
        String onDutyStaffs = onDuty.stream().map(r -> r.getRec("staff_id").getString("present")).collect(Collectors.joining(","));
        action.setValue("on_duty", onDutyStaffs);
        return action;
    }
}
