package jmaa.modules.md.label.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValueException;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "print.rule.material_label", label = "物料标签打印规则", inherit = {"print.rule"})
public class PrintRuleMaterialLabel extends AbstractModel {
    /**
     * 获取打印字段
     *
     * @param rec
     * @return
     */
    public List<String> getFields(Records rec) {
        return Arrays.asList("code", "sn", "material_code", "material_name", "material_spec", "lot_num", "product_lot", "supplier_name",
            "supplier_chars", "supplier_code", "qty", "unit", "product_date", "overdue_date", "print_time");
    }

    /**
     * 获取打印数据
     *
     * @param rec
     * @return
     */
    @Model.ServiceMethod(label = "获取打印数据", auth = "read")
    public List<Map<String, Object>> getData(Records rec, Map<String, Object> data) {
        Records labels = (Records) data.get("labels");
        if (labels != null) {
            return getDataByLabel(rec, labels);
        }
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("data");
        if (result == null) {
            throw new ValueException("打印参数不正确");
        }
        for (Map<String, Object> map : result) {
            map.put("material_name", Utils.replaceAll((String) map.get("material_name"), ",", "，"));
            map.put("material_spec", Utils.replaceAll((String) map.get("material_spec"), ",", "，"));
            map.put("supplier_name", Utils.replaceAll((String) map.get("supplier_name"), ",", "，"));
        }
        return result;
    }

    public List<Map<String, Object>> getDataByLabel(Records rec, Records labels) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records label : labels) {
            //供应商信息
            Records supplier = label.getRec("supplier_id");
            Records material = label.getRec("material_id");
            Date overDue = label.getDate("overdue_date");
            Date productDate = label.getDate("product_date");
            Map<String, Object> map = new HashMap<>();
            map.put("code", label.get("sn") + "|" + material.getString("code") + "|" + label.get("qty"));
            map.put("sn", label.get("sn"));
            map.put("lot_num", label.get("lot_num"));
            map.put("material_code", material.getString("code"));
            map.put("material_name", Utils.replaceAll(material.getString("name"), ",", "，"));
            map.put("material_spec", Utils.replaceAll(material.getString("spec"), ",", "，"));
            map.put("unit", material.getRec("unit_id").get("name"));
            map.put("qty", label.get("qty"));
            map.put("product_lot", label.get("product_lot"));
            map.put("product_date", Utils.format(productDate, "yyyy-MM-dd"));
            map.put("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"));
            map.put("supplier_chars", supplier.getString("chars"));
            map.put("supplier_code", supplier.getString("code"));
            map.put("overdue_date", Utils.format(overDue, "yyyy-MM-dd"));
            map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            result.add(map);
        }
        return result;
    }
}
