package jmaa.modules.md.work.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "md.work_shift", label = "班制")
public class WorkShift extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field name = Field.Char().label("名称").required();
    static Field active = Field.Boolean().label("是否有效");
    static Field is_default = Field.Boolean().label("是否默认");
    static Field day_ids = Field.One2many("md.work_shift_day", "shift_id").label("班次时间");
    static Field time_ids = Field.One2many("md.work_shift_time", "shift_id").label("工作时间");

    @OnSaved({"active", "is_default"})
    public void onDefaultSave(Records records) {
        for (Records record : records) {
            if (record.getBoolean("is_default")) {
                if (!record.getBoolean("active")) {
                    throw new ValidationException(record.l10n("无效状态不能设置为默认"));
                }
                records.getEnv().getCursor().execute("update md_work_shift set is_default=%s where id!=%s",
                    Utils.asList(false, record.getId()));
                record.getEnv().getCache().invalidate();
            }
        }
    }
}
