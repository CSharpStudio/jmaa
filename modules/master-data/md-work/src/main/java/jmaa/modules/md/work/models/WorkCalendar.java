package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "md.work_calendar", label = "工作日历")
public class WorkCalendar extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field active = Field.Boolean().label("是否有效").defaultValue(true);
    static Field is_default = Field.Boolean().label("是否默认");
    static Field week_ids = Field.One2many("md.work_week", "calendar_id").label("周方案");

    @OnSaved("is_default")
    public void onDefaultSave(Records records) {
        for (Records record : records) {
            if (record.getBoolean("is_default")) {
                if (!record.getBoolean("active")) {
                    throw new ValidationException(record.l10n("无效状态不能设置为默认"));
                }
                records.getEnv().getCursor().execute("update md_work_calendar set is_default=%s where id!=%s",
                    Utils.asList(false, record.getId()));
                record.getEnv().getCache().invalidate();
            }
        }
    }
}
