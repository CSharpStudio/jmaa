package jmaa.modules.md.work.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.work_week", label = "周方案", authModel = "md.work_calendar", order = "begin_date desc")
@Model.UniqueConstraint(name = "begin_date_unique", fields = {"calendar_id", "begin_date"})
public class WorkWeek extends Model {
    static Field calendar_id = Field.Many2one("md.work_calendar").label("工作日历");
    static Field shift_id = Field.Many2one("md.work_shift").label("班制");
    static Field begin_date = Field.Date().label("启用日期");
    static Field mon = Field.Boolean().label("周一");
    static Field tue = Field.Boolean().label("周二");
    static Field wed = Field.Boolean().label("周三");
    static Field thu = Field.Boolean().label("周四");
    static Field fri = Field.Boolean().label("周五");
    static Field sat = Field.Boolean().label("周六");
    static Field sun = Field.Boolean().label("周日");
}
