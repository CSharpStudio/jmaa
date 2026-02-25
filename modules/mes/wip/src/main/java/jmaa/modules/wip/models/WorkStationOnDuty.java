package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Constants;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "mfg.work_station_on_duty", label = "岗位值班", logAccess = BoolState.False)
@Model.UniqueConstraint(name = "station_user_unique", fields = {"station_id", "staff_id"}, message = "不能重复上岗")
public class WorkStationOnDuty extends Model {
    static Field station_id = Field.Many2one("md.work_station").label("工位").required();
    static Field staff_id = Field.Many2one("md.staff").label("员工").required();
    static Field create_uid = Field.Many2one("rbac.user").label("创建人");
    static Field create_date = Field.DateTime().label("创建时间");

    @Override
    public Map<String, Object> addMissingDefaultValues(Records rec, Map<String, Object> values) {
        Map<String, Object> result = (Map<String, Object>) callSuper(rec, values);
        result.put("create_uid", rec.getEnv().getUserId());
        result.put("create_date", new ServerDate());
        return result;
    }

    /**
     * 员工上岗
     */
    public void onDuty(Records records, String stationId, String staffId) {
        Records duty = records.find(Criteria.equal("staff_id", staffId));
        offDuty(duty);
        duty.create(new KvMap().set("station_id", stationId).set("staff_id", staffId));
        records.getEnv().get("mfg.work_station_on_duty_log").create(new KvMap().set("station_id", stationId).set("staff_id", staffId));
    }

    /**
     * 员工离岗
     */
    public void offDuty(Records records) {
        Records log = records.getEnv().get("mfg.work_station_on_duty_log");
        for (Records duty : records) {
            log.create(new KvMap()
                .set("status", "off")
                .set("station_id", duty.getRec("station_id").getId())
                .set("staff_id", duty.getRec("staff_id").getId()));
        }
        records.delete();
    }

    @ServiceMethod(auth = Constants.ANONYMOUS)
    public Object loadOnDuty(Records record, List<String> fields) {
        List<Object> result = new ArrayList<>();
        Records staff = record.getEnv().get("md.staff").find(Criteria.equal("account_id", record.getEnv().getUserId()));
        Records onDuty = record.find(Criteria.in("staff_id", staff.getIds()));
        return onDuty.read(fields);
    }

    @ServiceMethod(auth = Constants.ANONYMOUS)
    public Object userOffDuty(Records record) {
        Records account = record.getRec("staff_id").getRec("account_id");
        if (!Utils.equals(account.getId(), record.getEnv().getUserId())) {
            throw new ValidationException(record.l10n("用户[%s]没有员工[%s]操作权限",
                record.getEnv().getUser().get("present"), record.getRec("staff_id").get("present")));
        }
        offDuty(record);
        return Action.reload(record.l10n("操作成功"));
    }
}
