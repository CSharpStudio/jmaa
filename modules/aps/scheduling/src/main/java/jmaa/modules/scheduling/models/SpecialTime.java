package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.special_time", label = "特殊时间")
public class SpecialTime extends Model {
    static Field date = Field.Date().label("日期");
    static   Field is_rest = Field.Boolean().label("是否休息");
    static   Field start_hour = Field.Boolean().label("开始时间");
    static   Field end_hour = Field.Boolean().label("结束时间");
    static   Field is_overtime = Field.Boolean().label("是否加班");
    static   Field remark = Field.Char().label("备注");
    static Field resource_id = Field.Many2one("md.work_resource").label("资源");

}
