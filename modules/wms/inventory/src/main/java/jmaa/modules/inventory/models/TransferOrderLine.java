package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Set;
import java.util.stream.Collectors;

@Model.Meta(name = "wms.transfer_order_line", label = "仓库调拨需求明细", authModel = "wms.transfer_order", inherit = {"mixin.material"})
public class TransferOrderLine extends Model {
    static Field transfer_order_id = Field.Many2one("wms.transfer_order").label("调拨单号");
    static Field request_qty = Field.Float().label("需求数").required().min(0D).defaultValue(0);
    static Field transfer_qty = Field.Float().label("调拨数").defaultValue(0);
    static Field status = Field.Selection(new Options() {{
        put("new", "未调拨");
        put("transfering", "调拨中");
        put("transfered", "已备齐");
        put("done", "已完成");
    }}).label("状态").readonly(true).defaultValue("new");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发料仓库");

    @OnSaved("request_qty")
    public void onRequestQtySaved(Records records) {
        Records transferOrder = records.first().getRec("transfer_order_id");
        Set<String> baseWarehouseList = transferOrder.getRec("source_warehouse_ids").stream().map(Records::getId).collect(Collectors.toSet());
        for (Records record : records) {
            Double requestQty = record.getDouble("request_qty");
            if (Utils.less(requestQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]调拨数量必须大于0", record.getRec("material_id").get("code")));
            }
            // 查一下这个物料的已经扫描的数量
            Records line = record.getRec("transfer_order_id").getRec("line_ids");
            double qty = line.stream().mapToDouble(e -> e.getDouble("transfer_qty")).sum();
            if (Utils.less(requestQty, qty)) {
                throw new ValidationException(record.l10n("物料[%s]需求数不能小于已调拨数[%s]", record.getRec("material_id").get("code"), qty));
            }
            Double transferQty = record.getDouble("transfer_qty");
            if (Utils.largeOrEqual(transferQty, requestQty)) {
                record.set("status", "transfered");
            }
            Records warehouses = (Records) record.getRec("material_id").call("findWarehouses");
            Set<String> materialWarehouseList = warehouses.stream().map(Records::getId).collect(Collectors.toSet());
            materialWarehouseList.retainAll(baseWarehouseList);
            if (materialWarehouseList.isEmpty()) {
                record.set("warehouse_id", null);
            } else {
                record.set("warehouse_id", materialWarehouseList.iterator().next());
            }
        }
    }

    @OnSaved("transfer_qty")
    public void onIssueQtySaved(Records records) {
        for (Records record : records) {
            Double issueQty = record.getDouble("transfer_qty");
            Double requestQty = record.getDouble("request_qty");
            if (Utils.largeOrEqual(issueQty, requestQty)) {
                record.set("status", "transfered");
            } else if (Utils.large(issueQty, 0)) {
                record.set("status", "transfering");
            }
        }
    }

    @OnDelete
    public void deleteLine(Records records) {
        for (Records line : records) {
            Records details = line.getEnv().get("wms.transfer_order_details").find(Criteria.equal("transfer_order_id", line.getRec("transfer_order_id").getId()).and(Criteria.equal("material_id", line.getRec("material_id").getId())));
            if (details.any()) {
                throw new ValidationException(line.l10n("物料[%s]存在调拨明细数据,请先删除明细数据,再删除", line.getRec("material_id").get("code")));

            }
        }
    }
}
