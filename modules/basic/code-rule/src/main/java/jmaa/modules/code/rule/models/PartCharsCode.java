package jmaa.modules.code.rule.models;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * @author eric
 */
@Model.Meta(name = "code.part.chars_code", description = "固定的字符", label = "固定编码", logAccess = BoolState.False, authModel = "code.coding")

public class PartCharsCode extends ValueModel {
    static Field code_part_id = Field.Char().label("编码规则组成");
    static Field code = Field.Char().label("编码");

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
        String code = jsonObject.getString("code");
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
        return getPartCode(rec, partId, qty);
    }

    @ServiceMethod(auth = "read", doc = "根据编码规则组成显示")
    public String getDisPlay(Records rec, Map<String, String> partData) {
        return String.format("编码:%s", partData.get("code"));
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
        ruleMap.put("code", contentArray[1]);
        return ruleMap;
    }
}
