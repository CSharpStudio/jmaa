package jmaa.modules.inventory.models;

import org.jmaa.sdk.Doc;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.ValueModel;

import java.util.Map;

@Model.Meta(name = "wms.location_move_mobile", inherit = "wms.location_move", label = "库位移动-移动端")
@Model.Service(remove = "@edit")
public class LocationMoveMobile extends ValueModel {
    @ServiceMethod(label = "查找库存物料", auth = "read")
    public Map<String, Object> findMaterialByCode(Records records, @Doc("物料条码") String code) {
        return (Map<String, Object>) callSuper(records, code);
    }

    @ServiceMethod(label = "移库", auth = "read")
    public Object move(Records records, @Doc("物料条码") String code, @Doc("仓库id") String warehouseId, @Doc("源库位") String locationCodeSource, @Doc("库位条码") String locationCodeTarget, @Doc("数量") double qty) {
        return callSuper(records, code, warehouseId, locationCodeSource, locationCodeTarget, qty);
    }
}
