package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_delivery_mobile", label = "销售发货-移动端", inherit = {"wms.sales_delivery", "wms.sales_delivery_dialog"})
public class SalesDeliveryMobile extends ValueModel {
    static Field material_id = Field.Many2one("md.material").label("物料编码").readonly().store(false);
    static Field unit_id = Field.Many2one("md.unit").label("单位").related("material_id.unit_id").store(false);

    @ServiceMethod(label = "销售发货列表", doc = "发货单号、物料编码、标签条码查询收料单", auth = "read")
    public Object searchOrder(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        Environment env = record.getEnv();
        Criteria criteria = new Criteria();
        if (Utils.isNotEmpty(keyword)) {
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                criteria.and(Criteria.equal("line_ids.material_id", material.getId()).and("status", "=", "approve"));
            } else {
                // 成品标签/包装标签
                Records mdPackage = null;
                Records productLabel = env.get("lbl.product_label").find(Criteria.equal("sn", keyword));
                if (productLabel.any()) {
                    material = productLabel.getRec("material_id");
                } else if ((mdPackage = env.get("packing.package").find(Criteria.equal("code", keyword))).any()) {
                    material = mdPackage.getRec("material_id");
                }
                if (material != null && material.any()) {
                    criteria.and(Criteria.equal("line_ids.material_id", material.getId()).and("status", "=", "approve"));
                } else {
                    criteria.and(Criteria.in("status", Arrays.asList("approve", "done")).and(Criteria.equal("code", keyword)));
                }
            }
        } else {
            criteria.and("status", "=", "approve");
        }
        return env.get("wms.sales_delivery").search(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待调拨物料，根据出库规则推荐物料标签")
    public Object loadDeliveryMaterial(Records record, @Doc("仓库") String warehouseId, @Doc("偏移量") Integer offset) {
        return record.getEnv().get("wms.sales_delivery", record.getId()).call("loadDeliveryMaterial", warehouseId, offset);
    }

    @ServiceMethod(label = "扫码", auth = "read")
    public Object scanCode(Records record, String code, String warehouseId, Boolean autoConfirm) {
        return record.getEnv().get("wms.sales_delivery", record.getId()).call("scanCode", code, warehouseId, autoConfirm);
    }

    @ServiceMethod(label = "扫码确认", doc = "扫码以后确认功能", auth = "read")
    public Object deliveryMaterial(Records record, String code, String warehouseId, String locationId) {
        return record.getEnv().get("wms.sales_delivery", record.getId()).call("deliveryMaterial", code, warehouseId, locationId);
    }
    @ServiceMethod(label = "出库", doc = "根据发货明细生成出库单")
    public Object stockOut(Records records) {
        return records.getEnv().get("wms.sales_delivery", records.getId()).call("stockOut");
    }
}
