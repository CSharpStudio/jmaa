package jmaa.modules.md.packing.models;

import org.jmaa.sdk.*;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "print.rule.package", label = "包装打印规则", inherit = {"print.rule"})
public class PrintRulePackage extends AbstractModel {
    public List<String> getFields(Records rec) {
        return Arrays.asList("code", "supplier_chars", "supplier_code", "supplier_name", "material_code", "material_name", "material_spec", "unit", "qty", "packing_level");
    }

    @Model.ServiceMethod(auth = "read")
    public List<Map<String, Object>> getData(Records rec, Map<String, Object> data) {
        Records pkg = (Records) data.get("package");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Records row : pkg) {
            Map<String, Object> map = new HashMap<>();
            Records material = row.getRec("material_id");
            Records supplierRec = row.getRec("supplier_id");
            String supplierName = supplierRec.getString("name");
            map.put("code", row.get("code"));
            map.put("material_code", material.get("code"));
            map.put("material_name", Utils.replaceAll(material.getString("name"), ",", "，"));
            map.put("material_spec", Utils.replaceAll(material.getString("spec"), ",", "，"));
            map.put("supplier_name", Utils.replaceAll(supplierName, ",", "，"));
            map.put("supplier_chars", supplierRec.getString("chars"));
            map.put("supplier_code", supplierRec.getString("code"));
            map.put("unit", material.getRec("unit_id").get("name"));
            map.put("qty", row.get("qty"));
            map.put("packing_level", row.getSelection("packing_level"));
            result.add(map);
        }
        return result;
    }
}
