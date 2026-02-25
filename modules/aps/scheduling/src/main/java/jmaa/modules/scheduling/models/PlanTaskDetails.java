package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;

import java.util.HashSet;
import java.util.Set;

@Model.Meta(name = "as.plan_task_details", label = "计划任务明细", authModel = "as.plan_task")
public class PlanTaskDetails extends Model {
    static Field task_id = Field.Many2one("as.plan_task").label("计划任务");
    static Field craft_order_id = Field.Many2one("as.craft_order").label("制程单");
    static Field craft_process_id = Field.Many2one("md.craft_process").related("craft_order_id.craft_process_id");
    static Field product_order_id = Field.Many2one("mfg.product_order").related("craft_order_id.product_order_id");
    static Field plan_qty = Field.Float().label("计划数量");
    static Field cycle_time = Field.Integer().label("周期(秒/单位)");
    static Field efficiency = Field.Float().label("效率");
    static Field work_order_id = Field.Many2one("mfg.work_order");
    static Field output = Field.Float().related("work_order_id.output");
    static Field release_start = Field.DateTime().label("下达开始时间");
    static Field release_end = Field.DateTime().label("下达结束时间");
    static Field material_id = Field.Many2one("md.material").related("craft_order_id.product_id");
    static Field material_ready_date = Field.Date().related("craft_order_id.material_ready_date");
    static Field next_order_id = Field.Many2one("as.craft_order").related("craft_order_id.next_order_id");
    static Field next_relationship = Field.Selection().related("craft_order_id.next_relationship");

    @OnSaved("plan_qty")
    public void onPlanQtySaved(Records records) {
        Set<String> craftOrderIds = new HashSet<>();
        for (Records record : records) {
            craftOrderIds.add(record.getRec("craft_order_id").getId());
        }
        records.getEnv().get("as.craft_order", craftOrderIds).call("updateToPlanQty");
    }

    @Override
    public boolean delete(Records records) {
        Set<String> craftOrderIds = new HashSet<>();
        for (Records record : records) {
            craftOrderIds.add(record.getRec("craft_order_id").getId());
        }
        boolean result = (Boolean) callSuper(records);
        records.getEnv().get("as.craft_order", craftOrderIds).call("updateToPlanQty");
        return result;
    }
}

