package jmaa.modules.md.label.models;

import org.jmaa.sdk.AbstractModel;
import org.jmaa.sdk.Model;
import org.jmaa.sdk.Records;
import org.jmaa.sdk.Utils;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.exceptions.ValueException;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "print.rule.product_label", label = "产品标签打印规则", inherit = {"print.rule"})
public class PrintRuleProductLabel extends AbstractModel {
    /**
     * 获取打印字段
     *
     * @param rec
     * @return
     */
    public List<String> getFields(Records rec) {
        return Arrays.asList("sn", "work_order_code", "material_code", "material_name", "material_spec", "lot_num", "product_lot", "supplier_name",
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
            String supplierName = "";
            String supplierChars = "";
            String supplierCode = "";
            Records supplierRec = label.getRec("supplier_id");
            if (supplierRec.any()) {
                supplierName = supplierRec.getString("name");
                supplierChars = supplierRec.getString("chars");
                supplierCode = supplierRec.getString("code");
            }
            Records material = label.getRec("material_id");
            Date overDue = label.getDate("overdue_date");
            Date productionDate = label.getDate("product_date");
            Map<String, Object> map = new HashMap<>();
            // 经常有选错模板的,友好提示
            boolean workOrderFlag = label.getMeta().getFields().containsKey("work_order_id");
            if (workOrderFlag && label.getRec("work_order_id").any()) {
                map.put("work_order_code", label.getRec("work_order_id").getString("code"));
            } else if (workOrderFlag && !label.getRec("work_order_id").any()) {
                throw new ValidationException(rec.l10n("标签无关联工单数据字段,请检查标签模板"));
            }
            map.put("material_code", material.getString("code"));
            map.put("material_name", Utils.replaceAll(material.getString("name"), ",", "，"));
            map.put("material_spec", Utils.replaceAll(material.getString("spec"), ",", "，"));
            map.put("lot_num", label.get("lot_num"));
            map.put("sn", label.get("sn"));
            map.put("unit", material.getRec("unit_id").get("name"));
            map.put("qty", label.get("qty"));
            map.put("product_lot", label.get("product_lot"));
            map.put("product_date", Utils.format(productionDate, "yyyy-MM-dd"));
            map.put("supplier_name", Utils.replaceAll(supplierName, ",", "，"));
            map.put("supplier_chars", supplierChars);
            map.put("supplier_code", supplierCode);
            map.put("overdue_date", Utils.format(overDue, "yyyy-MM-dd"));
            map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            result.add(map);
        }
        return result;
    }
}
