package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "md.work_shift_time", label = "有效工作时间", authModel = "md.work_shift", order = "next_day,start_time")
public class WorkShiftTime extends Model {
    static Field shift_id = Field.Many2one("md.work_shift").label("班制").ondelete(DeleteMode.Cascade);
    static Field day_id = Field.Many2one("md.work_shift_day").label("班次").ondelete(DeleteMode.Cascade);
    static Field start_time = Field.Char().label("开始时间");
    static Field end_time = Field.Char().label("结束时间");
    static Field next_day = Field.Boolean().label("是否跨日").defaultValue(false);
    static Field is_ot = Field.Boolean().label("是否加班").defaultValue(false);
}
