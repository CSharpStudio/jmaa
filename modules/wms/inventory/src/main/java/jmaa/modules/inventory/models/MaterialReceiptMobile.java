package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author : eric
 * @Date 2023/3/11 17:22
 * @Description
 */
@Model.Meta(name = "wms.material_receipt_mobile", inherit = "wms.material_receipt", table = "wms_material_receipt", label = "移动端收料")
@Model.Service(remove = "@edit")
public class MaterialReceiptMobile extends ValueModel {
    static Field stock_rule = Field.Selection(Selection.related("md.material", "stock_rule")).store(false).label("库存规则");

    @ServiceMethod(label = "读取收料单列表", doc = "根据单号、条码或者箱号查询收料单", auth = "read")
    public Object searchReceipt(Records record, String keyword, Collection<String> fields, Integer limit, Integer offset, String order) {
        if (Utils.isNotEmpty(keyword)) {
            Environment env = record.getEnv();
            String[] codes = (String[]) env.get("lbl.code_parse").call("parse", keyword);
            if (codes.length > 1) {
                Criteria criteria = Criteria.equal("line_ids.material_id.code", codes[1])
                    .and("status", "=", "approve");
                return record.search(fields, criteria, offset, limit, order);
            }
            String type = (String) record.getEnv().get("code.matcher").call("match", keyword);
            if (Utils.isEmpty(type) || "packing.package".equals(type)) {
                Records pkg = record.getEnv().get("packing.package").find(Criteria.equal("code", keyword));
                if (pkg.any()) {
                    Criteria criteria = Criteria.equal("line_ids.material_id", pkg.getRec("material_id").getId())
                        .and("status", "=", "approve");
                    return record.search(fields, criteria, offset, limit, order);
                }
            }
            if (Utils.isEmpty(type) || "md.material".equals(type)) {
                Records material = record.getEnv().get("md.material").find(Criteria.equal("code", keyword));
                if (material.any()) {
                    Criteria criteria = Criteria.equal("line_ids.material_id", material.getId()).and("status", "=", "approve");
                    return record.search(fields, criteria, offset, limit, order);
                }
            }
            Criteria criteria = Criteria.in("status", Arrays.asList("approve", "done"))
                .and(Criteria.like("code", keyword).or(Criteria.like("delivery_note", keyword)));
            return record.search(fields, criteria, offset, limit, order);
        }
        return record.search(fields, Criteria.equal("status", "approve"), offset, limit, order);
    }

    @ServiceMethod(label = "扫描条码", auth = "read")
    public Object receiptByCode(Records record,
                                @Doc("标签条码/物料编码") String code,
                                @Doc("操作：none/auto/confirm") String action,
                                @Doc("收货数量(含赠品)") double receiveQty,
                                @Doc("赠品数量") double giftQty) {
        return callSuper(record, code, action, receiveQty, giftQty);
    }

    @ServiceMethod(label = "创建入库单", auth = "read")
    public Object createStockIn(Records records, Collection<String> materialIds, String comment) {
        return records.getEnv().get("wms.material_receipt", records.getIds()).call("createStockIn", materialIds, comment);
    }
}
