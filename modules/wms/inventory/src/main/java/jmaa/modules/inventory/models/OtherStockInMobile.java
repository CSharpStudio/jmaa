package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Collection;

@Model.Service(remove = "@edit")
@Model.Meta(name = "wms.other_stock_in_mobile", label = "其它入库-移动端", inherit = "wms.other_stock_in", table = "wms_other_stock_in")
public class OtherStockInMobile extends ValueModel {

    @ServiceMethod(label = "读取其它入库单列表", doc = "其它入库单号、物料编码、标签条码查询收料单", auth = "read")
    public Object searchOrder(Records record, String keyword,Criteria criteria, Collection<String> fields, Integer limit, Integer offset, String order) {
        if (Utils.isNotEmpty(keyword)) {
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                criteria.and(Criteria.equal("line_ids.material_id", material.getId()));
            } else {
                // 解析标签
                String[] codes = (String[]) record.getEnv().get("lbl.code_parse").call("parse", keyword);
                if (codes.length > 1) {
                    criteria.and(Criteria.equal("line_ids.material_id.code", codes[1]));
                } else {
                    criteria.and(Criteria.like("code", keyword));
                }
            }
        }
        return record.search(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(label = "扫描条码", doc = "物料编码则查询入库需求，物料标签则入库", auth = "read")
    public Object scanCode(Records record, String code, Boolean autoConfirm, String warehouseId, String locationId) {
        return record.getEnv().get("wms.other_stock_in", record.getId()).call("scanCode", code, autoConfirm, warehouseId, locationId);
    }

    @ServiceMethod(label = "扫描以后,确定", auth = "read")
    public Object submitScanCode(Records record, String materialId, String sn, Double qty, String warehouseId, String locationId) {
        return record.getEnv().get("wms.other_stock_in", record.getId()).call("receive", materialId, sn, qty, warehouseId, locationId);
    }

    @ServiceMethod(label = "出库", doc = "根据入库明细生成出库单", auth = "read")
    public Object stockIn(Records records) {
        return records.getEnv().get("wms.other_stock_in", records.getIds()).call("stockIn");
    }

    @ServiceMethod(auth = "read", label = "删除已扫描的标签")
    public void deleteDetail(Records records) {
        records.getEnv().get("wms.other_stock_in_details", records.getIds()).call("delete");
    }
    @ServiceMethod(auth = "read", label = "校验库位")
    public String checkLocation(Records record, String locationCode) {
        Records warehouseId = record.getRec("warehouse_id");
        if (Utils.isNotEmpty(locationCode)) {
            Criteria criteria = Criteria.equal("warehouse_id", warehouseId.getId());
            criteria.and(Criteria.equal("code", locationCode));
            Records location = record.getEnv().get("md.store_location").find(criteria);
            if (!location.any()) {
                throw new ValidationException(record.l10n("库位[%s]非当前仓库库位", locationCode));
            }
            return location.getId();
        }
        return null;
    }
}
