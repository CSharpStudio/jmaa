package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;


@Model.Meta(name = "wms.pick_out_mobile", label = "挑选-移动端", inherit = {"wms.pick_out"})
public class PickOutMobile extends ValueModel {

    @ServiceMethod(auth = "read")
    public List<Map<String, Object>> searchPickOutList(Records rec, Collection<String> fields, Integer offset, Integer limit, String order, String keyword) {
        Environment env = rec.getEnv();
        Criteria criteria = Criteria.in("status", Arrays.asList("draft","reject"));
        if (Utils.isNotBlank(keyword)) {
            // 输入来料检验单/挑选单/物料编码
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", keyword);
            if (codes.length > 1) {
                // 物料编码
                Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
                criteria.and(Criteria.equal("material_id", material.get("id")));
            } else {
                // 其他标签
                //来料检验单/挑选单
                criteria.and(Criteria.equal("code", keyword).or(Criteria.equal("material_id.code", keyword)).or(Criteria.equal("related_code", keyword)));
            }
        }
        return rec.getEnv().get("wms.pick_out").search(fields, criteria, offset, limit, order);
    }

    @Override
    @ServiceMethod(auth = "read")
    public List<Map<String, Object>> read(Records rec, Collection<String> fields) {
        return rec.getEnv().get("wms.pick_out", rec.getId()).read(fields);
    }

    @ServiceMethod(auth = "read", label = "扫描物料标签")
    public Map<String, Object> scanMaterialCode(Records record, String code, Integer qty) {
        // 这里的标签,可能是序列号,批次 ,  可能是asn单打印的标签, 也可能是其他地方打印的无任何关联的标签,
        // 码盘  RGF2505300024   packing.package
        // sn 标签  RGF2505300024|200110040001   sn|code lbl.material_label
        // 批次 标签  RGF2505300024-1|200110040001|RGF2505300024|100   sn|code lbl.lot_num | qty
        // 数量管控 RGF2505300024|200110040001|100      sn|code|qty
        Environment env = record.getEnv();
        return (Map<String, Object>) env.get("wms.pick_out", record.getId()).call("scanMaterialCode", code, qty);
    }

    /*@ServiceMethod(auth = "read", label = "提交")
    public void submitPick(Records record, String pickType) {
        Environment env = record.getEnv();
        env.get("wms.pick_out", record.getId()).call("submitPick", pickType);
    }*/

    @Model.ServiceMethod(label = "提交", doc = "提交单据，状态改为已提交",auth = "read")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        return records.getEnv().get("wms.pick_out",records.getId()).call("commit", values, comment);
    }

    @Override
    @ServiceMethod(auth = "read")
    public Map<String, Object> searchByField(Records rec, String relatedField, Criteria criteria, Integer offset, Integer limit, Collection<String> fields, String order) {
        return (Map<String, Object>) rec.getEnv().get("wms.pick_out", rec.getId()).call("searchByField", relatedField, criteria, offset, limit, fields, order);
    }

    @ServiceMethod(auth = "read", label = "删除扫描数据")
    public void deleteDetail(Records record, String detailId) {
        Records pickOutDetail = record.getEnv().get("wms.pick_out_details", detailId);
        if (!"new".equals(pickOutDetail.getString("status"))) {
            throw new ValidationException(record.l10n("当前标签状态,不允许删除"));
        }
        pickOutDetail.delete();
    }

}
