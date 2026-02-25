package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "md.work_shift_overtime", label = "班次加班时间", present = {"start_time", "end_time"},
    presentFormat = "{start_time}-{end_time}", authModel = "md.work_shift", order = "start_time")
public class WorkShiftOvertime extends Model {
    static Field day_id = Field.Many2one("md.work_shift_day").label("班次").ondelete(DeleteMode.Cascade);
    static Field remark = Field.Char().label("加班说明").required();
    static Field start_time = Field.Char().label("开始时间").required();
    static Field end_time = Field.Char().label("结束时间").required();

    @OnSaved({"start_time", "end_time"})
    public void onTimeSave(Records records) {
        for (Records record : records) {
            String start = record.getString("start_time");
            String end = record.getString("end_time");
            if (Utils.equals(start, end)) {
                throw new ValidationException(record.l10n("结束时间不能等于开始时间"));
            }
        }
    }
}
