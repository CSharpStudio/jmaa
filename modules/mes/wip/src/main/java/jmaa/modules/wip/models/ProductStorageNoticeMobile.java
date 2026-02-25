package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.action.AttrAction;

import java.util.Collection;

@Model.Meta(inherit = "mfg.product_storage_notice_mobile")
public class ProductStorageNoticeMobile extends ValueModel {
    static Field work_order_id = Field.Many2one("mfg.work_order").label("工单");
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

    @ServiceMethod(label = "读取成品入库通知列表", doc = "单号、物料编码、标签条码查询", auth = "read")
    public Object searchProductOrder(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        Criteria criteria = new Criteria();
        // 标签不需要解析,就lbl.material_label
        if (Utils.isNotEmpty(keyword)) {
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                criteria.and(Criteria.equal("material_id", material.getId()).and("status", "!=", "approve"));
            } else {
                Records materialLabel = record.getEnv().get("lbl.material_label");
                material = materialLabel.getRec("material_id");
                if (material.any()) {
                    criteria.and(Criteria.equal("material_id", material.getId()).and("status", "!=", "approve"));
                } else {
                    criteria.and(Criteria.equal("code", keyword));
                }
            }
        } else {
            criteria.and("status", "!=", "done");
        }
        return record.getEnv().get("mfg.product_storage_notice").search(fields, criteria, offset, limit, order);
    }

}
