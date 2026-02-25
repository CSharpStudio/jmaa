package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;

import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "print.rule.blank_label", label = "空白标签打印规则", inherit = {"print.rule"})
public class PrintRuleBlankLabel extends AbstractModel {
    /**
     * 获取打印字段
     *
     * @param rec
     * @return
     */
    public List<String> getFields(Records rec) {
        return Arrays.asList("sn", "print_time");
    }

    /**
     * 获取打印数据
     *
     * @param rec
     * @return
     */
    public List<Map<String, Object>> getData(Records rec, Map<String, Object> data) {
        Records labels = (Records) data.get("labels");
        List<Map<String, Object>> result = new ArrayList<>();
        if (labels != null) {
            for (Records label : labels) {
                Map<String, Object> map = new HashMap<>();
                map.put("sn", label.get("sn"));
                map.put("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
                result.add(map);
            }
        }
        return result;
    }
}
