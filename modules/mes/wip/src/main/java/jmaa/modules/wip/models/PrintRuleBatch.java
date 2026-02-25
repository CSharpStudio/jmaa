package jmaa.modules.wip.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValueException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "print.rule.batch", label = "批次打印规则", inherit = {"print.rule"})
public class PrintRuleBatch extends AbstractModel {
    public List<String> getFields(Records rec) {
        return Arrays.asList("code", "work_order_code", "material_code", "material_name", "material_spec", "unit", "qty");
    }

    @Model.ServiceMethod(auth = "read")
    public List<Map<String, Object>> getData(Records rec, Map<String, Object> data) {
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("data");
        if (result == null) {
            throw new ValueException("打印参数不正确");
        }
        for (Map<String, Object> map : result) {
            map.put("material_name", Utils.replaceAll((String) map.get("material_name"), ",", "，"));
            map.put("material_spec", Utils.replaceAll((String) map.get("material_spec"), ",", "，"));
        }
        return result;
    }
}
