package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

@Model.Meta(name = "as.user_settings", label = "用户设置")
public class UserSettings extends Model {
    static Field user_id = Field.Many2one("rbac.user").label("用户").unique().required();
    static Field split_craft_order = Field.Boolean().label("是否自动拆单").help("生成计划单时是否启用订单拆分规则").defaultValue(false);
    static Field split_max_size = Field.Integer().label("任务最大数量").defaultValue(10000).required();
    static Field merge_craft_order = Field.Boolean().label("是否自动合单").help("生成计划单时是否启用订单合并规则").defaultValue(false);
    static Field merge_min_size = Field.Integer().label("任务最小数量").defaultValue(50).required();
    static Field merge_customer = Field.Boolean().label("客户相同").defaultValue(true);
    static Field merge_factory_due_date = Field.Boolean().label("工厂交期相同").defaultValue(true);
    static Field merge_material_spec = Field.Boolean().label("规格型号相同").defaultValue(true);
    static Field task_zoom = Field.Integer().label("日历缩放").defaultValue(1);
    static Field task_begin_offset = Field.Integer().label("日历前置天数").defaultValue(5).required().min(0).help("当天之前天数");
    static Field task_end_offset = Field.Integer().label("日历后置天数").defaultValue(30).required().min(5).help("当天之后天数");
    static Field task_minor_begin_offset = Field.Integer().label("微调前置天数").defaultValue(3).required().min(0);
    static Field task_minor_end_offset = Field.Integer().label("微调后置天数").defaultValue(7).required().min(0);
    static Field task_show_finished = Field.Boolean().label("显示已完成任务").defaultValue(false);
    static Field task_remove_gaps = Field.Boolean().label("自动消除空隙").defaultValue(true);
    static Field task_show_link = Field.Boolean().label("显示任务连线").defaultValue(true);
    static Field task_lock_related = Field.Boolean().label("任务关联锁").defaultValue(true).help("锁定/解锁关联任务");
    static Field task_move_linkage = Field.Boolean().label("关联任务联动").defaultValue(true).help("任务移动时，关联任务联动更新");
    static Field task_severe_delay_days = Field.Integer().label("严重延迟天数").defaultValue(5).required().help("任务显示为深红色");
    static Field task_advance_days = Field.Integer().label("提前天数").defaultValue(3).required().help("任务显示为浅蓝色");
    static Field task_severe_advance_days = Field.Integer().label("严重提前天数").defaultValue(5).required().help("任务显示为深蓝色");
    static Field task_line_height = Field.Selection(new Options() {{
        put("36", "36");
        put("45", "45");
        put("60", "60");
        put("90", "90");
    }}).label("行高").defaultValue("45").required();
    static Field hide_resource_ids = Field.Many2many("md.work_resource", "as_user_hide_resources", "setting_id", "resource_id").label("隐藏的制造资源");
    static Field task_show_animate = Field.Boolean().label("任务显示动画").defaultValue(true);
}
