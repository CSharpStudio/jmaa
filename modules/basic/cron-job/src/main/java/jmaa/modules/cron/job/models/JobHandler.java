package jmaa.modules.cron.job.models;

import org.jmaa.sdk.Field;
import org.jmaa.sdk.Model;
/**
 * @author : eric
 **/
@Model.Meta(name = "job.handler", label = "定时任务配置",present = {"name", "method"},presentFormat = "{name} ({method})")
public class JobHandler extends Model {
    static Field name = Field.Char().label("名称").required().unique();
    static Field method = Field.Char().label("执行方法").required().unique();
    static Field job_ids = Field.One2many("job.task","job_handler");
    static Field description = Field.Char().label("描述").help("说明");
}
