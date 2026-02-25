package jmaa.modules.purchase.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "purchase.order_line", label = "采购订单物料明细", order = "line_no,id", authModel = "purchase.order", inherit = "mixin.material")
@Model.UniqueConstraint(name = "line_no_unique", fields = {"po_id", "line_no"}, message = "订单明细行号不能重复")
public class PurchaseOrderLine extends Model {
    static Field po_id = Field.Many2one("purchase.order").label("采购订单").required().ondelete(DeleteMode.Cascade);
    static Field line_no = Field.Integer().label("行号").required();
    static Field purchase_qty = Field.Float().label("采购数量").required().min(0d);
    static Field deliver_date = Field.Date().label("交货日期").required(true);
    static Field receive_qty = Field.Float().label("收货数量").required().defaultValue(0);
    static Field stock_in_qty = Field.Float().label("入库数量").required().defaultValue(0);
    static Field return_qty = Field.Float().label("退货数量").required().defaultValue(0);
    static Field status = Field.Selection(new Options() {{
        put("new", "新建");
        put("receive", "已收货");
        put("done", "已完成");
        put("close", "关闭");
    }}).label("状态").defaultValue("new");
    static Field purchase_unit_id = Field.Many2one("md.unit").related("material_id.purchase_unit_id");
    static Field purchase_unit_accuracy = Field.Integer().label("采购单位精度").related("material_id.purchase_unit_id.accuracy");

    @Model.ActionMethod
    public Action onMaterialChange(Records record) {
        Records material = record.getRec("material_id");
        Records unit = material.getRec("unit_id");
        Records purchaseUnit = material.getRec("purchase_unit_id");
        return Action.attr().setValue("material_name_spec", material.get("name_spec"))
            .setValue("material_category", material.get("category"))
            .setValue("unit_id", unit.getPresent())
            .setValue("purchase_unit_id", purchaseUnit.getPresent())
            .setValue("purchase_unit_accuracy", purchaseUnit.get("accuracy"));
    }

    @Override
    public void update(Records records, Map<String, Object> values) {
        Object qty = values.get("purchase_qty");
        Object date = values.get("deliver_date");
        if (qty != null || date != null) {
            for (Records line : records) {
                String message = "";
                if (qty != null) {
                    message += "<li>" + records.l10n("采购数量: %s ⇨ %s", line.get("purchase_qty"), qty) + "</li>";
                }
                if (date != null) {
                    message += "<li>" + records.l10n("交货日期: %s ⇨ %s",
                        Utils.format(line.getDate("deliver_date"), "yyyy-MM-dd"), date) + "</li>";
                }
                line.getRec("po_id").call("trackMessage", records.l10n("修改订单明细行号 %s, 物料 %s: <ul>%s</ul>",
                    line.getInteger("line_no"),
                    line.getRec("material_id").get("present"),
                    message));
            }
        }
        callSuper(records, values);
    }

    @OnDelete
    public void logDelete(Records records) {
        for (Records line : records) {
            line.getRec("po_id").call("trackMessage", records.l10n("删除订单明细，物料 %s: 采购数量 %s",
                line.getRec("material_id").get("present"),
                line.get("purchase_qty")));
        }
    }

    @OnSaved("receive_qty")
    public void onReceiveQtySaved(Records records) {
        for (Records line : records) {
            Double qty = line.getDouble("purchase_qty");
            Double receivedQty = line.getDouble("receive_qty");
            if (Utils.largeOrEqual(receivedQty, qty)) {
                line.set("status", "receive");
            }
        }
    }

    @OnSaved("stock_in_qty")
    public void onStockInQtySaved(Records records) {
        for (Records line : records) {
            Double qty = line.getDouble("purchase_qty");
            Double stockInQty = line.getDouble("stock_in_qty");
            if (Utils.largeOrEqual(stockInQty, qty)) {
                line.set("status", "done");
                line.getRec("po_id").call("updatePurchaseOrderStatus");
            }
        }
    }

    @OnSaved("return_qty")
    public void onReturnedQtySave(Records records) {
        for (Records line : records) {
            Double qty = line.getDouble("return_qty");
            if (Utils.large(qty, 0)) {
                line.getRec("po_id").set("is_return", true);
            }
        }
    }

    /**
     * 导入数据后刷新采购单信息
     */
    @Override
    public Map<String, Integer> createOrUpdate(Records record, List<Map<String, Object>> values) {
        Set<String> poIds = new HashSet<>();
        for (Map<String, Object> row : values) {
            String poId = (String) row.get("po_id");
            if (Utils.isNotEmpty(poId)) {
                poIds.add(poId);
            }
        }
        Criteria filter = Criteria.in("id", poIds)
            .and(Criteria.notEqual("status", "draft"))
            .and(Criteria.notEqual("status", "reject"));
        Records po = record.getEnv().get("purchase.order").find(filter);
        if (po.any()) {
            List<String> errors = new ArrayList<>();
            for (Records row : po) {
                errors.add(row.l10n("采购订单[%s]状态为[%s]不允许修改", row.get("code"), row.getSelection("status")));
            }
            throw new ValidationException(errors.stream().collect(Collectors.joining("\r\n")));
        }
        return (Map<String, Integer>) callSuper(record, values);
    }
}
