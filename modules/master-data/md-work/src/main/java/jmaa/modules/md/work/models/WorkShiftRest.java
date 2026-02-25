package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "md.work_shift_rest", label = "班次休息时间", present = {"name", "start_time", "end_time"},
    presentFormat = "{name}({start_time}-{end_time})", authModel = "md.work_shift", order = "start_time")
public class WorkShiftRest extends Model {
    static Field day_id = Field.Many2one("md.work_shift_day").label("班次").ondelete(DeleteMode.Cascade);
    static Field name = Field.Char().label("名称").required();
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
