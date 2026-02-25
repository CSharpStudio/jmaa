package jmaa.modules.code.rule.models;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.*;

import java.util.*;
/**
 * @author eric
 */
@Model.Meta(name = "code.part.date_template", description = "年月日，如：yy-MM-dd", label = "日期编码", logAccess = BoolState.False, authModel = "code.coding")

public class PartDateTemplate extends ValueModel {
    static Field code_part_id = Field.Char().label("编码规则组成");
    static Field format_code = Field.Selection().selection(new LinkedHashMap<String, String>() {
        {
            put("yyMMdd", "yyMMdd");
            put("yyyyMMdd", "yyyyMMdd");
            put("yMMdd", "yMMdd");
            put("yyyyMM", "yyyyMM");
            put("yyMM", "yyMM");
            put("yyyyWW", "yyyyWW");
            put("yyWW", "yyWW");
            put("MMdd", "MMdd");
            put("MM", "MM");
            put("dd", "dd");
        }
    }).label("日期模板");

    /**
     * 生成编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> getPartCode(Records rec, String partId, int qty) {
        Records partRecord = rec.getEnv().get("code.part", partId);
        String content = (String) partRecord.get("content");
        JSONObject jsonObject = JSONObject.parseObject(content);

        String code = Utils.format(new Date(), jsonObject.getString("format_code"));
        return Collections.nCopies(qty, code);
    }

    /**
     * 生成预览编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> previewPartCode(Records rec, String partId, int qty) {
        return this.getPartCode(rec, partId, qty);
    }

    @ServiceMethod(auth = "read", doc = "根据编码规则组成显示")
    public String getDisPlay(Records rec, Map<String, String> partData) {
        String formatCode = partData.get("format_code");

        return String.format("时间格式:%s", formatCode);
    }

    /**
     * 把组成规则内容转成map对象
     *
     * @param content 组成规则内容
     * @return map对象
     */
    @ServiceMethod(auth = "read", doc = "把组成规则内容转成map对象")
    public Map<String, String> getRulePartData(Records rec, String content) {
        String[] contentArray = content.split(":");
        Map<String, String> ruleMap = new HashMap<>();
        ruleMap.put("format_code", contentArray[1]);
        return ruleMap;
    }
}
