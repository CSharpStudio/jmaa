package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Model.Meta(name = "mfg.material_issue", label = "生产发料", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class MaterialIssue extends Model {
    static Field code = Field.Char().label("发料单号").unique();
    static Field type = Field.Selection(new Options() {{
        put("mfg.work_order", "工单发料");
        put("maintain", "维修发料");
    }}).label("需求分类").required();
    static Field related_code = Field.Char().label("相关单据");
    static Field related_id = Field.Many2oneReference("type").label("相关单据");
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间");
    static Field warehouse_ids = Field.Many2many("md.warehouse", "mfg_material_issue_warehouse", "issue_id", "warehouse_id").label("发料仓库");
    static Field line_ids = Field.One2many("mfg.material_issue_line", "issue_id").label("发料需求");
    static Field remark = Field.Char().label("备注");

    /**
     * 提交，校验发货仓库
     */
    @ServiceMethod(label = "提交")
    public Object commit(Records records, Map<String, Object> values, String comment) {
        if (values != null) {
            records.update(values);
        }
        for (Records record : records) {
            Records warehouse = record.getRec("warehouse_ids");
            if (!warehouse.any()) {
                throw new ValidationException(records.l10n("发货仓库不能为空"));
            }
            Records lines = record.getRec("line_ids");
            if (!lines.any()) {
                throw new ValidationException(records.l10n("发料需求不能为空"));
            }
        }
        records.set("status", "commit");
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "commit");
        return Action.success();
    }

    @OnSaved("warehouse_ids")
    public void onWarehouseIdsSaved(Records record) {
        // 没提交之前都可以修改仓库
        // 第一次保存的时候,两个地方都会执行这段逻辑
        Records issueLine = record.getRec("line_ids");
        if (issueLine.any()) {
            Set<String> baseWarehouseList = record.getRec("warehouse_ids").stream().map(Records::getId).collect(Collectors.toSet());
            for (Records line : issueLine) {
                Records warehouses = (Records) line.getRec("material_id").call("findWarehouses");
                // 获取交集,并拿一个
                Set<String> materialWarehouseList = warehouses.stream().map(Records::getId).collect(Collectors.toSet());
                materialWarehouseList.retainAll(baseWarehouseList);
                if (materialWarehouseList.isEmpty()) {
                    line.set("warehouse_id", null);
                } else {
                    line.set("warehouse_id", materialWarehouseList.iterator().next());
                }
            }
        }
    }
}
