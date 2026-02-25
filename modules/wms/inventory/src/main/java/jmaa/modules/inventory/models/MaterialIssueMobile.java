package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@Model.Service(remove = "@edit")
@Model.Meta(name = "mfg.material_issue_mobile", label = "发料单-移动端", inherit = {"mfg.material_issue", "mfg.material_issue_dialog"}, table = "mfg_material_issue")
public class MaterialIssueMobile extends ValueModel {
    @ServiceMethod(label = "读取发料单列表", doc = "根据单号、条码或者箱号查询收料单", auth = "read")
    public Object searchIssueOrder(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        if (Utils.isNotEmpty(keyword)) {
            Environment env = record.getEnv();
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", keyword);
            if (codes.length > 1) {
                Criteria criteria = Criteria.equal("line_ids.material_id.code", codes[1])
                    .and("status", "=", "approve");
                return record.search(fields, criteria, offset, limit, order);
            }
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                Criteria criteria = Criteria.equal("line_ids.material_id", material.getId()).and("status", "=", "approve");
                return record.search(fields, criteria, offset, limit, order);
            } else {
                Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", keyword));
                if (label.any()) {
                    Criteria criteria = Criteria.equal("line_ids.material_id", label.getRec("material_id").getId()).and("status", "=", "approve");
                    return record.search(fields, criteria, offset, limit, order);
                }
            }
            Criteria criteria = Criteria.in("status", Arrays.asList("approve", "done"))
                .and(Criteria.like("code", keyword).or(Criteria.like("related_code", keyword)));
            return record.search(fields, criteria, offset, limit, order);
        }
        return record.search(fields, Criteria.equal("status", "approve"), offset, limit, order);
    }

    @ServiceMethod(label = "下一个物料", auth = "read", doc = "按库位顺序加载下一个待发物料，根据出库规则推荐物料标签")
    public Map<String, Object> loadIssueMaterial(Records record,
                                                 @Doc("仓库") String warehouseId,
                                                 @Doc("偏移量") Integer offset) {
        return (Map<String, Object>) record.getEnv().get("mfg.material_issue", record.getIds()).call("loadIssueMaterial", warehouseId, offset);
    }


    @ServiceMethod(label = "出库", auth = "read")
    public Object stockOut(Records records) {
        return records.getEnv().get("mfg.material_issue", records.getIds()).call("stockOut");
    }

    @ServiceMethod(label = "扫描条码", auth = "read")
    public Object issue(Records record, @Doc("标签条码/物料编码") String code) {
        return record.getEnv().get("mfg.material_issue", record.getIds()).call("issue", code);
    }

    @ServiceMethod(label = "发料", auth = "read")
    public Object issueMaterial(Records record,
                                @Doc("标签") String code,
                                @Doc("仓库") String warehouseId,
                                @Doc("库位编码") String locationCode,
                                @Doc("物料") String materialId,
                                @Doc("发料数量") double qty,
                                @Doc("是否打印") boolean printLabel,
                                @Doc("标签模板") String templateId) {
        // pda 库位不下拉,换成扫码
        String locationId = null;
        if (Utils.isNotEmpty(locationCode)) {
            Records location = record.getEnv().get("md.store_location").find(Criteria.equal("code", locationCode).and(Criteria.equal("warehouse_id", warehouseId)));
            if (!location.any()) {
                throw new ValidationException(record.l10n("仓库[%s]不存在库位[%s]", record.getEnv().get("md.warehouse", warehouseId).get("present"), locationCode));
            }
            locationId = location.getId();
        }
        return record.getEnv().get("mfg.material_issue", record.getIds()).call("issueMaterial", code, warehouseId, locationId, materialId, qty, printLabel, templateId);
    }

    @ServiceMethod(label = "拆分标签发料", auth = "read")
    public Object splitLabel(Records record,
                             @Doc("条码") String sn,
                             @Doc("拆分数量") double splitQty,
                             @Doc("是否打印原标签") boolean printOld) {
        return record.getEnv().get("mfg.material_issue", record.getIds()).call("splitLabel", sn, splitQty, printOld);
    }
}
