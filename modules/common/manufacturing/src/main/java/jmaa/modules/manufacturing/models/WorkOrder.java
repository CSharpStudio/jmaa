package jmaa.modules.manufacturing.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;
import org.jmaa.sdk.exceptions.ValidationException;

@Model.Meta(name = "mfg.work_order", label = "生产工单", inherit = {"code.auto_code", "mixin.company", "mixin.material", "bbs.thread"})
public class WorkOrder extends Model {
    static Field code = Field.Char().label("工单号").unique();
    static Field material_id = Field.Many2one("md.material").label("产品").tracking();
    static Field plan_qty = Field.Float().label("计划数量").required().greaterThen(0D).tracking();
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").tracking();
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").tracking();
    static Field product_order_id = Field.Many2one("mfg.product_order").label("生产订单").tracking();
    static Field remark = Field.Char().label("备注");
    static Field status = Field.Selection(new Options() {{
        put("draft", "创建");
        put("release", "已发放");
        put("producing", "生产中");
        put("suspend", "暂停");
        put("close", "关闭");
        put("done", "完工");
    }}).defaultValue("draft").label("工单状态");
    static Field plan_start = Field.DateTime().label("计划开始时间").format("yyyy-MM-dd HH:mm");
    static Field plan_end = Field.DateTime().label("计划完成时间").format("yyyy-MM-dd HH:mm");
    static Field output = Field.Float().label("完工数量").defaultValue(0);

    @Model.ServiceMethod(label = "关闭", doc = "关闭单据，从任意状态改为关闭")
    public Object close(Records records, @Doc(doc = "关闭原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("close".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态已关闭"));
            }
        }
        String body = records.l10n("关闭") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "close");
        return Action.success();
    }

    @Model.ServiceMethod(label = "重新修改", doc = "重新修改，从任意状态改为草稿")
    public Object reopen(Records records, @Doc(doc = "重新修改原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("draft".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以重新修改", record.getSelection("status")));
            }
        }
        String body = records.l10n("重新修改") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "draft");
        return Action.success();
    }

    @ActionMethod
    public Object onProductOrderChange(Records record) {
        AttrAction action = new AttrAction();
        Records productOrder = record.getRec("product_order_id");
        if (productOrder.any()) {
            Records material = productOrder.getRec("material_id");
            Records unit = material.getRec("unit_id");
            action.setValue("material_id", material);
            action.setValue("material_name_spec", material.get("name_spec"));
            action.setValue("material_category", material.get("category"));
            action.setValue("unit_id", unit.getPresent());
            action.setValue("unit_accuracy", unit.get("accuracy"));
            action.setValue("plan_qty", productOrder.getDouble("plan_qty"));
        }
        return action;
    }
}
