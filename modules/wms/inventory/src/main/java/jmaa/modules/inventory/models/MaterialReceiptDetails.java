package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;


/**
 * @author eric
 */
@Model.Meta(name = "wms.material_receipt_details", label = "收料明细", authModel = "wms.material_receipt", table = "stock_stock_in_details", inherit = {"stock.stock_in_details"})
public class MaterialReceiptDetails extends Model {
    static Field receipt_id = Field.Many2one("wms.material_receipt").label("收料单");
//    static Field receipt_status = Field.Selection(new Options() {{
//        put("received", "已收货");
//        put("done", "已完成");
//    }}).label("状态").defaultValue("received");
    static Field product_lot = Field.Char().label("生产批次").related("label_id.product_lot");
    static Field lpn = Field.Char().label("LPN");

    @Model.OnSaved("label_id")
    public void onLabelIdSaved(Records records) {
        for (Records record : records) {
            Records label = record.getRec("label_id");
            if (label.any()) {
                record.set("lpn", label.getRec("package_id").getString("code"));
                // 继承stock_in_details   --- >   继承 mixin.material_label.会覆盖 onsaved 要重写加上
                record.set("sn", label.getString("sn"));
            }
        }
    }

    @ServiceMethod(label = "补打条码")
    public Map<String, Object> reprint(Records records, Double printQty, Double minPackages) {
        // 序列号直接打印，批次和数量要提供打印数量计算打印张数
        Environment env = records.getEnv();
        Records material = records.first().getRec("material_id");
        String stockRule = material.getString("stock_rule");
        for (Records record : records) {
            if (!Utils.equals(material, record.getRec("material_id"))) {
                throw new ValidationException(records.l10n("不同物料不能一起补打"));
            }
        }
        if ("sn".equals(stockRule)) {
            List<String> labelIds = new ArrayList<>();
            for (Records record : records) {
                labelIds.add(record.getRec("label_id").getId());
            }
            Records labels = env.get("lbl.material_label", labelIds);
            Records printTemplate = labels.first().getRec("print_template_id");
            for (Records label : labels) {
                if (!Utils.equals(printTemplate.getId(), label.getRec("print_template_id").getId())) {
                    throw new ValidationException(records.l10n("补打的标签存在不同的打印模板"));
                }
            }
            return (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
                put("labels", labels);
            }});
        }
        Records detail = records.first();
        String lotNum = detail.getString("lot_num");
        for (Records record : records) {
            if (!Utils.equals(lotNum, record.getString("lot_num"))) {
                throw new ValidationException(records.l10n("不同物料批次号不能一起补打"));
            }
        }
        if (Utils.isNotEmpty(printQty)) {
            int labelCount = (int) Math.ceil(printQty / minPackages);
            List<String> codes = (List<String>) env.get("lbl.material_label").call("createCodes", material.getId(), labelCount);
            Records printTemplate = material.getRec("print_tpl_id");
            List<Map<String, Object>> labels = new ArrayList<>();
            Records lot = env.get("lbl.lot_num");
            if (Utils.isNotEmpty(lotNum)) {
                lot = lot.find(Criteria.equal("code", lotNum).and(Criteria.equal("material_id",material.getId())));
            }
            Records supplier = detail.getRec("receipt_id").getRec("supplier_id");
            double leftQty = printQty;
            for (String code : codes) {
                double qty = Utils.lessOrEqual(leftQty, minPackages) ? leftQty : minPackages;
                leftQty = Utils.round(leftQty - minPackages);
                Map<String, Object> map = new HashMap<>();
                map.put("sn", code);
                map.put("qty", qty);
                map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                map.put("supplier_name", supplier.getString("name"));
                map.put("supplier_chars", supplier.get("chars"));
                map.put("supplier_code", supplier.get("code"));
                map.put("material_code", material.getString("code"));
                map.put("material_name", material.getString("name"));
                map.put("material_spec", material.getString("spec"));
                map.put("unit", material.getRec("unit_id").get("name"));
                if ("num".equals(stockRule)) {
                    map.put("code", code + "|" + material.getString("code") + "|" + qty);
                } else {
                    map.put("product_date", Utils.format(lot.getDate("product_date"), "yyyy-MM-dd"));
                    map.put("code", code + "|" + material.getString("code") + "|" + lotNum + "|" + qty);
                    map.put("lot_num", lotNum);
                }
                labels.add(map);
            }
            return (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
                put("data", labels);
            }});
        }
        Map<String, Object> result = new HashMap<>();
        result.put("min_packages", material.getDouble("min_packages"));
        double qty = records.stream().mapToDouble(r -> r.getDouble("qty")).sum();
        result.put("qty", qty);
        return result;
    }
}
