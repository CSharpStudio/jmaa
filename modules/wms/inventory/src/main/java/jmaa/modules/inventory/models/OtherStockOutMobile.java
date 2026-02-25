package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Collection;

@Model.Service(remove = "@edit")
@Model.Meta(name = "wms.other_stock_out_mobile", label = "其它出库-移动端", inherit = {"wms.other_stock_out", "wms.other_stock_out_dialog"})
public class OtherStockOutMobile extends ValueModel {

    @ServiceMethod(label = "读取其它出库单列表", doc = "其它出库单号、物料编码、标签条码查询收料单", auth = "read")
    public Object searchOrder(Records record,Criteria criteria, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
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
        return record.getEnv().get("wms.other_stock_out").search(fields, criteria, offset, limit, order);
    }

    @ServiceMethod(label = "发货", doc = "物料编码则查询出库需求，物料标签则出库", auth = "read")
    public Object delivery(Records record, @Doc("标签条码/物料编码") String code) {
        return record.getEnv().get("wms.other_stock_out", record.getId()).call("delivery", code);
    }

    @ServiceMethod(label = "出库", doc = "数量管控的物料进行出库", auth = "read")
    public Object stockOutMaterial(Records record, @Doc("标签") String code, @Doc("仓库") String warehouseId,
                                   @Doc("库位") String locationId, @Doc("物料") String materialId, @Doc("出库数量") Double qty,
                                   @Doc("是否打印") Boolean printFlag, @Doc("标签模板") String templateId) {
        return record.getEnv().get("wms.other_stock_out", record.getId()).call("stockOutMaterial", code, warehouseId, locationId, materialId, qty, printFlag, templateId);
    }

    @ServiceMethod(label = "出库", doc = "根据出库明细生成出库单", auth = "read")
    public Object stockIn(Records records) {
        return records.getEnv().get("wms.other_stock_out", records.getIds()).call("stockIn");
    }

    @ServiceMethod(auth = "read", label = "删除已扫描的标签")
    public void deleteDetail(Records records) {
        records.getEnv().get("wms.other_stock_out_details", records.getIds()).call("delete");
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

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待发物料，根据出库规则推荐物料标签")
    public Object loadStockOutMaterial(Records record, @Doc("仓库") String warehouseId, @Doc("偏移量") Integer offset) {
        return record.getEnv().get("wms.other_stock_out", record.getIds()).call("loadStockOutMaterial", warehouseId, offset);
    }

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签", auth = "read")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        return record.getEnv().get("wms.other_stock_out", record.getIds()).call("splitLabel", sn, splitQty, printOld);
    }
    @ServiceMethod(label = "出库", doc = "根据出库明细生成出库单",auth = "read")
    public Object stockOut(Records record) {
        return record.getEnv().get("wms.other_stock_out", record.getIds()).call("stockOut");
    }
}
