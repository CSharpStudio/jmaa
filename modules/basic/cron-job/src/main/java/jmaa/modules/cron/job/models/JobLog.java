package jmaa.modules.cron.job.models;

import org.jmaa.sdk.BoolState;
import org.jmaa.sdk.DeleteMode;
import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;

import java.util.HashMap;

/**
 * @author : eric
 **/
@Model.Meta(name = "job.log", label = "定时任务日志", order = "start_time desc", authModel = "job.task", logAccess = BoolState.False)
public class JobLog extends Model {
    static Field status = Field.Selection().label("调度结果").selection(new HashMap<String, String>() {{
        put("run", "操作中");
        put("success", "成功");
        put("error", "失败");
    }});
    static Field start_time = Field.DateTime().label("执行开始时间");
    static Field end_time = Field.DateTime().label("执行结束时间");
    static Field error = Field.Char().label("错误信息").length(4000);
    static Field content = Field.Text().label("日志信息");
    static Field job_id = Field.Many2one("job.task").ondelete(DeleteMode.Cascade);
}
