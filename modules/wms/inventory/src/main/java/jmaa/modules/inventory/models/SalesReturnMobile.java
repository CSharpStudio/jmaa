package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;

import java.util.Arrays;
import java.util.Collection;

@Model.Service(remove = "@edit")
@Model.Meta(name = "wms.sales_return_mobile", label = "销售退货-移动端", inherit = {"wms.sales_return", "wms.sales_return_dialog"}, table = "wms_sales_return")
public class SalesReturnMobile extends ValueModel {

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
            criteria.and("status", "!=", "done");
        }
        return env.get("wms.sales_return").search(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(label = "扫描标签", doc = "序列号直接退,其他显示明细", auth = "read")
    public Object scanCode(Records record, String code, Boolean autoConfirm, String warehouseId) {
        return record.getEnv().get("wms.sales_return", record.getId()).call("scanCode", code, autoConfirm, warehouseId);
    }

    @Model.ServiceMethod(label = "收货", doc = "扫码以后确认功能", auth = "read")
    public Object receive(Records record, @Doc("仓库") String warehouseId, @Doc("标签") String code, @Doc("数量") Double qty) {
        return record.getEnv().get("wms.sales_return", record.getId()).call("receive", warehouseId, code, qty);
    }

    @ServiceMethod(label = "入库", doc = "根据退货明细生成入库单")
    public Object stockIn(Records records) {
        return records.getEnv().get("wms.sales_return", records.getIds()).call("stockIn");
    }
}
