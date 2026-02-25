package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.services.CreateService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Model.Meta(name = "mfg.product_storage_notice_mobile", label = "成品入库通知-移动端", inherit = {"mfg.product_storage_notice"})
@Model.Service(remove = "@edit")
@Model.Service(name = "create", label = "创建", auth = "read", description = "为模型创建新记录", type = CreateService.class)
public class ProductStorageNoticeMobile extends ValueModel {

    @ServiceMethod(label = "读取成品入库通知列表", doc = "单号、物料编码、标签条码查询", auth = "read")
    public Object searchProductOrder(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        Criteria criteria = new Criteria();
        // 标签不需要解析,就lbl.material_label
        if (Utils.isNotEmpty(keyword)) {
            Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
            if (material.any()) {
                criteria.and(Criteria.equal("material_id", material.getId()).and("status", "!=", "done"));
            } else {
                Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", keyword));
                material = label.getRec("material_id");
                if (material.any()) {
                    criteria.and(Criteria.equal("material_id", material.getId()).and("status", "!=", "done"));
                } else {
                    criteria.and(Criteria.equal("code", keyword).or("related_code", "=", keyword));
                }
            }
        } else {
            criteria.and("status", "!=", "done");
        }
        return record.getEnv().get("mfg.product_storage_notice").search(fields, criteria, offset, limit, order);
    }

    @Override
    public Records createBatch(Records rec, List<Map<String, Object>> valuesList) {
        return rec.getEnv().get("mfg.product_storage_notice").createBatch(valuesList);
    }

    @ServiceMethod(label = "扫描标签", doc = "序列号直接退,其他显示明细", auth = "read")
    public Object scanCode(Records record, String code, boolean submit) {
        return record.getEnv().get("mfg.product_storage_notice", record.getId()).call("scanCode", code, submit);
    }

    @ServiceMethod(label = "提交", doc = "提交单据，状态改为已提交", auth = "read")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        return records.getEnv().get("mfg.product_storage_notice", records.getId()).call("commit", values, comment);
    }
}
