package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;

@Model.Meta(name = "as.craft_order", label = "制程单", inherit = "code.auto_code", order = "id desc")
@Model.Service(remove = {"create", "createBatch", "copy", "delete"})
public class CraftOrder extends Model {
    static Field code = Field.Char().label("编码").required().unique();
    static Field product_order_id = Field.Many2one("mfg.product_order").label("生产订单").required();
    static Field factory_due_date = Field.Date().related("product_order_id.factory_due_date");
    static Field customer_id = Field.Many2one("md.customer").related("product_order_id.customer_id");
    static Field craft_process_id = Field.Many2one("md.craft_process").label("制程名称").required();
    static Field material_id = Field.Many2one("md.material").label("产品");
    static Field material_name_spec = Field.Char().related("material_id.name_spec");
    static Field plan_qty = Field.Float().label("计划数量").required().greaterThen(0d);
    static Field status = Field.Selection(new Options() {{
        put("new", "未排");
        put("plan", "已排");
        put("release", "下达");
    }}).label("状态").defaultValue("new");
    static Field next_relationship = Field.Selection(Selection.related("md.craft_route_node", "next_relationship")).label("前后关系");
    static Field transfer_batch = Field.Integer().label("转运批量");
    static Field transfer_time = Field.Integer().label("转运时间(秒)");
    static Field interval_days = Field.Integer().label("间隔天数");
    static Field material_ready_date = Field.Date().label("齐料日期");
    static Field next_order_id = Field.Many2one("as.craft_order").label("后制程单").ondelete(DeleteMode.SetNull);
    static Field to_plan_qty = Field.Float().label("未排数量").required().defaultValue(0);
    static Field generate_status = Field.Selection(new Options() {{
        put("new", "新增");
        put("success", "生成成功");
        put("error", "生成失败");
    }}).label("生成状态").store(false);
    static Field quota_ids = Field.One2many("as.craft_order_quota", "order_id").label("工时定额");
    static Field product_id = Field.Many2one("md.material").label("产品").compute("computeProduct");

    public Object computeProduct(Records record) {
        Records product = record.getRec("material_id");
        if (product.any()) {
            return product.getId();
        }
        return record.getRec("product_order_id").getRec("material_id").getId();
    }

    @OnSaved({"plan_qty"})
    public void updateToPlanQty(Records records) {
        Cursor cr = records.getEnv().getCursor();
        String sql = "select sum(plan_qty) from as_plan_task_details where craft_order_id=%s";
        for (Records record : records) {
            double planQty = record.getDouble("plan_qty");
            cr.execute(sql, Arrays.asList(record.getId()));
            double taskQty = Utils.toDouble(cr.fetchOne()[0]);
            if (Utils.large(taskQty, planQty)) {
                throw new ValidationException(records.l10n("制程单[%s]任务数量[%s]大于计划数量[%s]",
                    record.get("code"), taskQty, planQty));
            }
            record.set("to_plan_qty", planQty - taskQty);
        }
    }
}
