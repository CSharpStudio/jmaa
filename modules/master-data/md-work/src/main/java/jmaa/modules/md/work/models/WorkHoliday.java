package jmaa.modules.md.work.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

@Model.Meta(name = "md.work_holiday", label = "法定假期")
public class WorkHoliday extends Model {
    static Field name = Field.Char().label("名称").required();
    static Field start_date = Field.Date().label("开始日期").required();
    static Field end_date = Field.Date().label("结束日期").required();
}
