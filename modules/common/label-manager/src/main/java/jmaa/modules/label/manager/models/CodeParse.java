package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.Map;

@Model.Meta(name = "lbl.code_parse", label = "标签拆分记录", authModel = "lbl.material_label")
public class CodeParse extends ValueModel {
    /**
     * 解析条码：
     * <pre>
     *     序列号条码:[sn,material],
     *     批次条码:[seq,material,lot,qty]
     *     数量条码：[sn,material,qty]
     * </pre>
     */
    public String[] parse(Records records, String code) {
        if (Utils.isEmpty(code)) {
            throw new ValidationException("code不能为空");
        }
        // 本地标签 是
        // LYI251022001|100110080002|LYI251022001|100
        // 100110120003|100110120003|100
        // RYI25102220000|110700120001
        // 匹配标签
        Map<String, Object> valueMap = (Map) records.getEnv().get("code.matcher").call("labelMatch", code);
        if (null == valueMap || valueMap.isEmpty()) {
            // 全部解析不出来, 给个保底
            // return code.replaceAll("\\uFEFF", "").split("\\|");
            // 保底乱报错, 顺序不同,下面获取物料就会报错, 除非保底 =1
            String[] split = code.replaceAll("\\uFEFF", "").split("\\|");
            if (split.length > 1) {
                throw new ValidationException(records.l10n("标签[%s]无法解析,请检查标签数据或添加编码规则", code));
            }
            return split;
        }
        String type = Utils.toString(valueMap.get("type"));
        if ("lbl.material_label".equals(type)) {
            if (Utils.isNotEmpty(valueMap.get("code"))) {
                // 这种情况就是 物料标签类型 直接设置 sn 号, 长度给了 1  比如成品标签,包装标签等, 给个保底
                return code.replaceAll("\\uFEFF", "").split("\\|");
            }
            if (Utils.toBoolean(valueMap.get("isCreate"))) {
                matchGenerate(records, valueMap);
            }
            return new String[]{Utils.toString(valueMap.get("sn")), Utils.toString(valueMap.get("materialCode")), Utils.toString(valueMap.get("lotNum")), Utils.toString(valueMap.get("qty"))};
            //} else if ("md.store_location".equals(type) || "packing.package".equals(code) || "lbl.product_label".equals(type)) {
            // todo 其他类型的,后续补充逻辑,现在先避免 长度数组  > 1
            //return new String[]{valueMap.get("code")};
        } else {
            // todo 其他规则待补充
            return new String[]{Utils.toString(valueMap.get("code"))};
        }
    }

    /**
     * 匹配过滤
     *
     * @param records
     * @param valueMap
     * @return
     */
    public void matchGenerate(Records records, Map<String, Object> valueMap) {
    }
}
