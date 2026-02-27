package jmaa.modules.scheduling.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.HashMap;
import java.util.Map;

@Model.Meta(inherit = "mfg.product_order")
public class ProductOrder extends Model {
    static Field craft_order_ids = Field.One2many("as.craft_order", "product_order_id").label("制程单");
    static Field status = Field.Selection().addSelection(new Options() {{
        put("craft", "已生成");
    }});
    static Field priority = Field.Integer().label("优先级").defaultValue(100);
    static Field craft_route_id = Field.Many2one("md.craft_route").label("制程路线");

    @OnSaved("plan_qty")
    public void onPlanQtySave(Records records) {
        for (Records record : records) {
            Records orders = record.getRec("craft_order_ids");
            if (orders.any()) {
                for (Records order : orders) {
                    if (!"draft".equals(order.getString("status"))) {
                        throw new ValidationException(record.l10n("制程单[%s]状态[%s]不能修改数量", order.get("code"), order.getSelection("status")));
                    }
                }
                orders.set("plan_qty", record.get("plan_qty"));
            }
        }
    }

    @OnSaved("material_id")
    public void onMaterialSave(Records records) {
        for (Records record : records) {
            Records route = record.getRec("craft_route_id");
            Records material = record.getRec("material_id");
            if (material.any() && !route.any()) {
                record.set("craft_route_id", material.getRec("craft_route_id").getId());
            }
        }
    }

    @ServiceMethod(label = "生成制程单")
    public Object createCraftOrder(Records records) {
        for (Records record : records) {
            if (!"approve".equals(record.getString("status"))) {
                throw new ValidationException(record.l10n("生产订单[%s]状态[%s]不能生成制程单", record.get("code"), record.getSelection("status")));
            }
            Records craftRoute = record.getRec("craft_route_id");
            if (!craftRoute.any()) {
                throw new ValidationException(record.l10n("订单[%s]未设置制程路线", record.get("code")));
            }
            if (!craftRoute.getBoolean("active")) {
                throw new ValidationException(record.l10n("制程路线[%s]未生效", craftRoute.get("present")));
            }
            Records material = record.getRec("material_id");
            Records routeNodes = craftRoute.getRec("node_ids");
            Records node = routeNodes.filter(n -> n.getBoolean("is_start")).firstOrDefault();
            Records productRouteBom = record.getEnv().get("md.craft_route_bom").find(Criteria.equal("product_id.material_id", material.getId())
                .and("product_id.craft_route_id", "=", craftRoute.getId()).and("product_id.is_default", "=", true));
            Map<String, String> nodeIds = new HashMap<>();
            do {
                Records next = node.getRec("next_id");
                if (next.any()) {
                    nodeIds.put(next.getId(), node.getId());
                }
                node = next;
            } while (node.any());
            node = routeNodes.filter(n -> n.getBoolean("is_end")).firstOrDefault();
            Records order = records.getEnv().get("as.craft_order");
            int idx = nodeIds.size() + 1;
            do {
                KvMap toCreate = new KvMap()
                    .set("code", record.get("code") + String.format("-%02d", idx--))
                    .set("product_order_id", record.getId())
                    .set("plan_qty", record.get("plan_qty"))
                    .set("to_plan_qty", record.get("plan_qty"))
                    .set("craft_process_id", node.getRec("process_id").getId())
                    .set("transfer_batch", node.get("transfer_batch"))
                    .set("transfer_time", node.get("transfer_time"))
                    .set("interval_days", node.get("interval_days"));
                if (node.getBoolean("is_end")) {
                    toCreate.set("material_id", material.getId());
                } else {
                    toCreate.set("next_relationship", node.get("next_relationship"))
                        .set("next_order_id", order.getId());
                    Records n = node;
                    Records bom = productRouteBom.filter(r -> r.getRec("node_id").equals(n)).firstOrDefault();
                    toCreate.set("material_id", bom.getRec("material_id").getId());
                }
                order = order.create(toCreate);
                String nextId = nodeIds.get(node.getId());
                if (Utils.isEmpty(nextId)) {
                    break;
                } else {
                    node = node.browse(nextId);
                }
            } while (true);
        }
        records.set("status", "craft");
        return Action.reload(records.l10n("操作成功"));
    }

    @ServiceMethod(label = "撤销制程单")
    public Object removeCraftOrder(Records records) {
        for (Records record : records) {
            Records order = record.getRec("craft_order_ids");
            for (Records row : order) {
                String status = row.getString("status");
                if (!"new".equals(status)) {
                    throw new ValidationException(record.l10n("制程单[%s]状态[%s]不能撤销", row.get("code"), row.getSelection("status")));
                }
            }
            order.delete();
        }
        records.set("status", "new");
        return Action.reload(records.l10n("操作成功"));
    }
}
