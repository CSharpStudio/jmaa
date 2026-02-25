package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.BinaryOp;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

@Model.Meta(name = "lbl.label_print", label = "标签打印记录", inherit = "mixin.material")
public class LabelPrint extends Model {
    static Field stock_rule = Field.Selection().related("material_id.stock_rule");
    static Field template_id = Field.Many2one("print.template").label("标签模板").required();
    static Field min_packages = Field.Float().label("标签数量").required();
    static Field print_qty = Field.Integer().label("打印数量").required();
    static Field label_count = Field.Integer().label("标签张数");
    static Field product_date = Field.Date().label("生产日期").required();
    static Field product_lot = Field.Char().label("生产批次");
    static Field lot_attr = Field.Char().label("批次属性");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field lpn = Field.Char().label("LPN");
    static Field start_label_code = Field.Char().label("开始条码");
    static Field end_label_code = Field.Char().label("结束条码");
    static Field label_code_range = Field.Char().label("标签范围").compute("computeLabelRange");
    static Field print_count = Field.Integer().label("补打次数").defaultValue(0);
    static Field lot_num = Field.Char().label("批次号");

    public String computeLabelRange(Records record) {
        return record.getString("start_label_code") + "~" + record.getString("end_label_code");
    }

    @Override
    public List<Map<String, Object>> search(Records rec, Collection<String> fields, Criteria criteria, Integer offset, Integer limit, String order) {
        boolean admin = rec.getEnv().isAdmin();
        if (!admin) {
            Records userRec = rec.getEnv().getUser();
            Records supplierRec = userRec.getRec("supplier_ids");
            if (supplierRec.any()) {
                criteria.and(Criteria.in("supplier_id", supplierRec.getIds()));
            }
        }
        return (List<Map<String, Object>>) callSuper(rec, fields, criteria, offset, limit, order);
    }

    @Model.ServiceMethod(label = "标签补打")
    public Map<String, Object> reprintLabel(Records records) {
        Records printTemplate = records.first().getRec("template_id");
        for (Records record : records) {
            if (!Utils.equals(printTemplate.getId(), record.getRec("template_id").getId())) {
                throw new ValidationException(records.l10n("补打的标签存在不同的打印模板"));
            }
        }
        List<Object> labels = new ArrayList<>();
        for (Records record : records) {
            record.set("print_count", record.getInteger("print_count") + 1);
            Records material = record.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records materialLabel = record.getEnv().get("lbl.material_label").find(Criteria.equal("label_print_id", record.getId()));
                labels.addAll((List<Map<String, Object>>) ((Map<String, Object>) materialLabel.call("reprintLabel")).get("data"));
                continue;
            }
            double minPackages = record.getDouble("min_packages");
            double printQty = record.getDouble("print_qty");
            String lotNum = record.getString("lot_num");
            int count = (int) Math.ceil(printQty / minPackages);
            List<String> codes = (List<String>) record.getEnv().get("lbl.material_label").call("createCodes", material.getId(), count);
            double leftQty = printQty;
            Records supplier = record.getRec("supplier_id");
            for (String code : codes) {
                double qty = Utils.lessOrEqual(leftQty, minPackages) ? leftQty : minPackages;
                leftQty = Utils.round(leftQty - minPackages);
                Map<String, Object> map = new HashMap<>();
                map.put("lpn", record.get("lpn"));
                map.put("material_code", material.getString("code"));
                map.put("material_name", material.getString("name"));
                map.put("material_spec", material.getString("spec"));
                map.put("unit", material.getRec("unit_id").get("name"));
                map.put("supplier_name", supplier.getString("name"));
                map.put("supplier_chars", supplier.get("chars"));
                map.put("supplier_code", supplier.get("code"));
                map.put("product_lot", record.get("product_lot"));
                map.put("lot_attr", record.get("lot_attr"));
                map.put("product_date", Utils.format(record.getDate("product_date"), "yyyy-MM-dd"));
                map.put("qty", qty);
                map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                if ("num".equals(stockRule)) {
                    map.put("code", code + "|" + material.getString("code") + "|" + qty);
                } else {
                    map.put("code", code + "|" + material.getString("code") + "|" + lotNum + "|" + qty);
                    map.put("lot_num", lotNum);
                }
                map.put("sn", code);
                labels.add(map);
            }
        }
        return (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
            put("data", labels);
        }});
    }

    /**
     * 打印标签
     */
    @Model.ServiceMethod(label = "标签打印")
    public Map<String, Object> printLabel(Records records,
                                          @Doc("物料ID") String materialId,
                                          @Doc("标签数量") double minPackages,
                                          @Doc("打印数量") double printQty,
                                          @Doc("生产周期") Date productDate,
                                          @Doc("生产批次") String productLot,
                                          @Doc("批次属性") String lotAttr,
                                          @Doc("打印模板") String printTplId,
                                          @Doc("最外层包装") String lpn,
                                          @Doc("其它参数") Map<String, Object> data) {
        Environment env = records.getEnv();
        return (Map<String, Object>) env.get("lbl.material_label").call("printLabel", materialId, minPackages, printQty, productDate, productLot, lotAttr, printTplId, lpn, data);
    }
}
