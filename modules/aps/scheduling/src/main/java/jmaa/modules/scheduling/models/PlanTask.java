package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.plan_task", label = "计划任务", inherit = "code.auto_code")
public class PlanTask extends Model {
    static Field code = Field.Char().label("任务编码");
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源");
    static Field craft_type_id = Field.Many2one("md.craft_type").label("制程类型");
    static Field algorithm = Field.Selection().related("craft_type_id.algorithm");
    static Field is_locked = Field.Boolean().label("是否锁定").defaultValue(false);
    static Field is_warning = Field.Boolean().label("是否警告").defaultValue(false);
    static Field plan_qty = Field.Float().label("计划数量");
    static Field output_qty = Field.Float().label("完工数量").defaultValue(0);
    static Field duration = Field.Float().label("生产周期(分钟)");
    static Field transfer_time = Field.Integer().label("转款时间(秒)").defaultValue(0);
    static Field factory_due_date = Field.Date().label("工厂交期");
    static Field status = Field.Selection(new Options() {{
        put("new", "未完成");
        put("release", "已下达");
        put("done", "已完成");
    }}).label("状态").defaultValue("new");
    static Field plan_start = Field.DateTime().label("计划开始");
    static Field plan_end = Field.DateTime().label("计划结束");
    static Field work_start = Field.DateTime().label("开工时间");
    static Field mold_id = Field.Many2one("md.sub_resource").label("辅助资源");
    static Field efficiency = Field.Float().label("效率(%)").defaultValue(1);
    static Field details_ids = Field.One2many("as.plan_task_details", "task_id").label("任务明细");

    @OnDelete
    public void deleteTask(Records records) {
        for (Records record : records) {
            record.getRec("details_ids").delete();
        }
    }
}
