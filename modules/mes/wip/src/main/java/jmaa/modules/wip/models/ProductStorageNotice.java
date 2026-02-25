package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

/**
 * @author eric
 */
@Model.Meta(inherit = "mfg.product_storage_notice")
public class ProductStorageNotice extends Model {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
    static Field material_id = Field.Many2one("md.material").label("产品编码").related("work_order_id.material_id");
    static Field plan_qty = Field.Float().label("计划数量").related("work_order_id.plan_qty");
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间").related("work_order_id.workshop_id");
    static Field resource_id = Field.Many2one("md.work_resource").label("制造资源").related("work_order_id.resource_id");
    static Field product_order_id = Field.Many2one("mfg.product_order").label("生产订单").related("work_order_id.product_order_id");
    static Field output = Field.Float().label("完工数量").related("work_order_id.output");

    @ActionMethod
    public Action onWorkOrderIdChange(Records rec) {
        AttrAction action = new AttrAction();
        Records workOrderId = rec.getRec("work_order_id");
        if (workOrderId.any()) {
            action.setValue("material_id", workOrderId.getRec("material_id"));
            action.setValue("plan_qty", workOrderId.get("plan_qty"));
            action.setValue("workshop_id", workOrderId.get("workshop_id"));
            action.setValue("resource_id", workOrderId.get("resource_id"));
            action.setValue("product_order_id", workOrderId.get("product_order_id"));
            action.setValue("output", workOrderId.get("output"));
        }
        return action;
    }

    @OnSaved("work_order_id")
    public void onWorkOrderIdSave(Records records) {
        for (Records record : records) {
            Records workOrderId = record.getRec("work_order_id");
            if (workOrderId.any()) {
                record.set("related_code", workOrderId.getString("code"));
            }
        }
    }
}
