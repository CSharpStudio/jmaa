package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.util.ServerDate;

import java.util.Map;

@Model.Meta(name = "mfg.work_station_on_duty_log", label = "上岗记录", logAccess = BoolState.False)
public class WorkStationOnDutyLog extends Model {
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field staff_id = Field.Many2one("md.staff").label("员工").required();
    static Field status = Field.Selection(new Options() {{
        put("on", "上岗");
        put("off", "下岗");
    }}).label("状态").defaultValue("on");
    static Field create_uid = Field.Many2one("rbac.user").label("创建人");
    static Field create_date = Field.DateTime().label("创建时间");

    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Map<String, Object> result = (Map<String, Object>) callSuper(rec, values);
        result.put("create_uid", rec.getEnv().getUserId());
        result.put("create_date", new ServerDate());
        return result;
    }
}
