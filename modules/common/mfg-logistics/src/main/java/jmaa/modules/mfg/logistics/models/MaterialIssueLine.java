package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Set;
import java.util.stream.Collectors;

@Model.Meta(name = "mfg.material_issue_line", label = "发料需求", authModel = "mfg.material_issue", inherit = "mixin.material")
public class MaterialIssueLine extends Model {
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
    static Field status = Field.Selection(new Options() {{
        put("new", "未发料");
        put("issuing", "发料中");
        put("issued", "已发料");
        put("done", "已完成");
    }}).label("行状态").readonly(true).defaultValue("new");
    static Field issue_id = Field.Many2one("mfg.material_issue").label("发料单");
    static Field request_qty = Field.Float().label("需求数").required().min(0D);
    static Field issued_qty = Field.Float().label("已发料数").required().defaultValue(0);
    // 如果物料配置为任意,则取一个,如果物料配置有仓库 且 发货仓库 有   则限制当前仓库,  发货仓库未选择,则 null
    static Field warehouse_id = Field.Many2one("md.warehouse").label("发料仓库");

    @OnSaved("request_qty")
    public void onRequestQtySaved(Records records) {
        // 这里只能是同一单的需求数据
        Records issue = records.first().getRec("issue_id");
        Set<String> baseWarehouseList = issue.getRec("warehouse_ids").stream().map(Records::getId).collect(Collectors.toSet());
        // 仓库可能有多条 可能没有数据,那就给默认的
        for (Records record : records) {
            Double requestQty = record.getDouble("request_qty");
            if (Utils.less(requestQty, 0)) {
                throw new ValidationException(record.l10n("物料[%s]需求数量必须大于0", record.getRec("material_id").get("code")));
            }
            Double issuedQty = record.getDouble("issued_qty");
            if (Utils.less(requestQty, issuedQty)) {
                throw new ValidationException(record.l10n("物料[%s]需求数量不能小于已发数量[%s]", record.getRec("material_id").get("code"), issuedQty));
            }
            if (Utils.largeOrEqual(issuedQty, requestQty)) {
                record.set("status", "issued");
            }
            Records warehouseId = record.getRec("warehouse_id");
            // 手动添加行物料的不处理
            if (!warehouseId.any()) {
                // 确定发料仓库
                Records warehouses = (Records) record.getRec("material_id").call("findWarehouses");
                // 获取交集,并拿一个
                Set<String> materialWarehouseList = warehouses.stream().map(Records::getId).collect(Collectors.toSet());
                materialWarehouseList.retainAll(baseWarehouseList);
                if (materialWarehouseList.isEmpty()) {
                    record.set("warehouse_id", null);
                } else {
                    record.set("warehouse_id", materialWarehouseList.iterator().next());
                }
            }

        }
    }


    @OnSaved("issued_qty")
    public void onIssueQtySaved(Records records) {
        for (Records record : records) {
            Double issueQty = record.getDouble("issued_qty");
            Double requestQty = record.getDouble("request_qty");
            if (Utils.largeOrEqual(issueQty, requestQty)) {
                record.set("status", "issued");
            } else if (Utils.large(issueQty, 0)) {
                record.set("status", "issuing");
            }
        }
    }
}
