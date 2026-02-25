package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;

import java.util.Map;

@Model.Meta(name = "wms.transfer_order_mobile", label = "仓库调拨-移动端", inherit = {"wms.transfer_order", "wms.transfer_order_dialog"}, table = "wms_transfer_order")
public class TransferOrderMobile extends ValueModel {

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待调拨物料，根据出库规则推荐物料标签")
    public Map<String, Object> loadTransferMaterial(Records record,
                                                    @Doc("仓库") String warehouseId,
                                                    @Doc("偏移量") Integer offset) {
        return (Map<String, Object>) record.getEnv().get("wms.transfer_order", record.getIds()).call("loadTransferMaterial", warehouseId, offset);
    }

    @ServiceMethod(label = "调拨", doc = "数量管控的物料进行调拨", auth = "read")
    public Object transferMaterial(Records record, @Doc("标签") String code, @Doc("仓库") String warehouseId,
                                   @Doc("库位") String locationId, @Doc("物料") String materialId, @Doc("调拨数量") Double qty,
                                   @Doc("是否打印") Boolean printFlag, @Doc("标签模板") String templateId) {
        return record.getEnv().get("wms.transfer_order", record.getIds()).call("transferMaterial", code, warehouseId, locationId, materialId, qty, printFlag, templateId);
    }
}
