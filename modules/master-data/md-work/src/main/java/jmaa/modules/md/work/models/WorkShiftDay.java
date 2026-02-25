package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.StringUtils;
import org.jmaa.sdk.util.KvMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "md.work_shift_day", label = "班次时间", authModel = "md.work_shift", order = "code")
public class WorkShiftDay extends Model {
    static Field shift_id = Field.Many2one("md.work_shift").label("班制").ondelete(DeleteMode.Cascade);
    static Field code = Field.Char().label("编码").required();
    static Field name = Field.Char().label("名称").required();
    static Field start_time = Field.Char().label("开始时间").required();
    static Field end_time = Field.Char().label("结束时间").required();
    static Field next_day = Field.Boolean().label("是否跨日").help("结束时间超过当天24时为跨日");
    static Field rest_ids = Field.One2many("md.work_shift_rest", "day_id").label("休息时间");
    static Field ot_ids = Field.One2many("md.work_shift_overtime", "day_id").label("加班时间");
    static Field time_ids = Field.One2many("md.work_shift_time", "day_id").label("工作时间");

    @OnSaved({"start_time", "end_time", "rest_ids", "ot_ids"})
    public void updateShiftTime(Records records) {
        Cursor cr = records.getEnv().getCursor();
        List<Map<String, Object>> toCreate = new ArrayList<>();
        for (Records record : records) {
            String start = record.getString("start_time");
            String end = record.getString("end_time");
            if (Utils.equals(start, end)) {
                throw new ValidationException(record.l10n("结束时间不能等于开始时间"));
            }
            String shiftId = record.getRec("shift_id").getId();
            String time = start;
            Records rest = record.getRec("rest_ids");
            Records overtime = record.getRec("ot_ids");
            boolean nextDay = StringUtils.compare(start, end) < 0 && record.getBoolean("next_day");
            for (Records row : rest) {
                String startRest = row.getString("start_time");
                String endRest = row.getString("end_time");
                if (StringUtils.compare(startRest, time) < 0) {
                    if (nextDay) {
                        throw new ValidationException(record.l10n("休息开始时间[%s]不能小于[%s]", startRest, time));
                    }
                    toCreate.add(new KvMap().set("shift_id", shiftId)
                        .set("day_id", record.getId())
                        .set("start_time", time + ":00")
                        .set("end_time", "23:59:59")
                        .set("next_day", nextDay));
                    time = "00:00";
                    nextDay = true;
                }
                if (!"00:00".equals(startRest)) {
                    toCreate.add(new KvMap().set("shift_id", shiftId)
                        .set("day_id", record.getId())
                        .set("start_time", time + ":00")
                        .set("end_time", startRest + ":00")
                        .set("next_day", nextDay));
                }
                if (StringUtils.compare(endRest, time) < 0) {
                    nextDay = true;
                }
                time = endRest;
            }
            for(Records row : overtime){

            }
            if (StringUtils.compare(end, time) < 0) {
                if (nextDay) {
                    throw new ValidationException(record.l10n("休息开始时间[%s]不能小于[%s]", end, time));
                }
                toCreate.add(new KvMap().set("shift_id", shiftId)
                    .set("day_id", record.getId())
                    .set("start_time", time + ":00")
                    .set("end_time", "23:59:59")
                    .set("next_day", nextDay));
                time = "00:00";
                nextDay = true;
            }
            toCreate.add(new KvMap().set("shift_id", shiftId)
                .set("day_id", record.getId())
                .set("start_time", time + ":00")
                .set("end_time", end + ":00")
                .set("next_day", nextDay));
//            toCreate.add(new KvMap().set("shift_id", shiftId)
//                .set("day_id", record.getId())
//                .set("start_time", time + ":00")
//                .set("end_time", end + ":00")
//                .set("is_ot", true));
        }
        cr.execute("delete from md_work_shift_time where day_id in %s", Utils.asList(Utils.asList(records.getIds())));
        records.getEnv().get("md.work_shift_time").createBatch(toCreate);
    }
}
