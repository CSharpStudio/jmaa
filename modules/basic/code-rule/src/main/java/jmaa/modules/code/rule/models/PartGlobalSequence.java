package jmaa.modules.code.rule.models;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.tools.IdWorker;

import java.util.*;
/**
 * @author eric
 */
@Model.Meta(name = "code.part.global_sequence", description = "全局流水号，一直累加序列", label = "全局序列号", logAccess = BoolState.False, authModel = "code.coding")

public class PartGlobalSequence extends Model {
    static Field start = Field.Integer().label("开始序号").store(false).defaultValue(1);
    static Field code_part_id = Field.Char().label("编码规则组成");
    static Field length = Field.Integer().label("编码长度").store(false);
    static Field step = Field.Integer().label("步长").store(false).defaultValue(1);
    static Field current_sequence = Field.Integer().label("当前序号");

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
        Integer start = jsonObject.getInteger("start");
        Integer step = jsonObject.getInteger("step");
        Integer length = jsonObject.getInteger("length");

        try (Cursor cr = rec.getEnv().getDatabase().openCursor()) {
            cr.execute("UPDATE code_part SET id=id WHERE id=%s", Collections.singleton(partId));
            cr.execute("SELECT current_sequence FROM code_part_global_sequence WHERE code_part_id=%s", Arrays.asList(partId));
            if (cr.getRowCount() > 0) {
                Object[] row = cr.fetchOne();
                int current = Utils.toInt(row[0]);
                List<String> codes = new ArrayList<>();
                for (int i = 0; i < qty; i++) {
                    String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
                    current = current + step;
                    codes.add(code);
                }
                cr.execute("UPDATE code_part_global_sequence SET current_sequence=%s WHERE code_part_id=%s",
                        Arrays.asList(current, partId));
                cr.commit();
                return codes;
            } else {
                int current = start;
                List<String> codes = new ArrayList<>();
                for (int i = 0; i < qty; i++) {
                    String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
                    current = current + step;
                    codes.add(code);
                }
                String id = IdWorker.nextId();
                cr.execute("INSERT INTO code_part_global_sequence(id,code_part_id,current_sequence) values(%s,%s,%s)",
                        Arrays.asList(id, partId, current));
                cr.commit();
                return codes;
            }
        }
    }

    /**
     * 生成预览编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> previewPartCode(Records rec, String partId, int qty) {
        Records partRecord = rec.getEnv().get("code.part", partId);
        String content = (String) partRecord.get("content");
        JSONObject jsonObject = JSONObject.parseObject(content);
        Integer start = jsonObject.getInteger("start");
        Integer step = jsonObject.getInteger("step");
        Integer length = jsonObject.getInteger("length");
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT current_sequence FROM code_part_global_sequence WHERE code_part_id=%s", Arrays.asList(partId));
        int current = start;
        if (cr.getRowCount() > 0) {
            Object[] row = cr.fetchOne();
            current = Utils.toInt(row[0]);
        }
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
            current = current + (step * (i + 1));
            codes.add(code);
        }
        return codes;
    }

    @ServiceMethod(auth = "read", doc = "根据编码规则组成显示")
    public String getDisPlay(Records rec, Map<String, String> partData) {
        return String.format("位数:%s;步长:%s", partData.get("place"), partData.get("step"));
    }
}
