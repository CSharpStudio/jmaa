package jmaa.modules.code.rule.models;

import com.alibaba.fastjson.JSONObject;
import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.tools.IdWorker;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author eric
 */
@Model.Meta(name = "code.part.day_sequence", description = "按日期生成流水号算法，每天重新编码", label = "按日序列号", inherit = "code.part.global_sequence", logAccess = BoolState.False, authModel = "code.coding")

public class PartDaySequence extends Model {

    static Field current_day = Field.Char().label("当前日期");

    /**
     * 生成编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> getPartCode(Records rec, String partId, int qty) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //当前日期
        String currentDate = dateFormat.format(new Date());
        Records partRecord = rec.getEnv().get("code.part", partId);
        String content = (String) partRecord.get("content");
        JSONObject jsonObject = JSONObject.parseObject(content);
        List<String> emptyCodes = new ArrayList<>();
        if(Utils.isEmpty(jsonObject)){
            return emptyCodes;
        }
        Integer start = jsonObject.getInteger("start");
        Integer step = jsonObject.getInteger("step");
        Integer length = jsonObject.getInteger("length");
        if(start == null || step == null || length== null){
            return emptyCodes;
        }
        //TODO 后期优化防止并发，要先对该行上锁，保证更新在同一个事务中
        try (Cursor cr = rec.getEnv().getDatabase().openCursor()) {
            cr.execute("UPDATE code_part SET id=id WHERE id=%s", Collections.singleton(partId));
            cr.execute("SELECT current_sequence, current_day FROM code_part_day_sequence WHERE code_part_id=%s", Arrays.asList(partId));
            if (cr.getRowCount() > 0) {
                Object[] row = cr.fetchOne();
                int current = Utils.toInt(row[0]);
                String current_day = Utils.toString(row[1]);
                if (!Objects.equals(currentDate, current_day)) {
                    current = start;
                }
                List<String> codes = new ArrayList<>();
                for (int i = 0; i < qty; i++) {
                    String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
                    current = current + step;
                    codes.add(code);
                }
                cr.execute("UPDATE code_part_day_sequence SET current_sequence=%s,current_day=%s WHERE code_part_id=%s",
                        Arrays.asList(current, currentDate, partId));
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
                cr.execute("INSERT INTO code_part_day_sequence(id,code_part_id,current_day,current_sequence) values(%s,%s,%s,%s)",
                        Arrays.asList(id, partId, currentDate, current));
                cr.commit();
                return codes;
            }
        } catch (Exception e) {
            throw new ValidationException(rec.l10n(e.toString()));
        }
    }

    /**
     * 生成编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> previewPartCode(Records rec, String partId, int qty) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //当前日期
        String currentDate = dateFormat.format(new Date());
        Records partRecord = rec.getEnv().get("code.part", partId);
        String content = (String) partRecord.get("content");
        JSONObject jsonObject = JSONObject.parseObject(content);
        Integer start = jsonObject.getInteger("start");
        Integer step = jsonObject.getInteger("step");
        Integer length = jsonObject.getInteger("length");
        Cursor cr = rec.getEnv().getCursor();
        cr.execute("SELECT current_sequence, current_day FROM code_part_day_sequence WHERE code_part_id=%s", Arrays.asList(partId));
        int current = start;
        if (cr.getRowCount() > 0) {
            Object[] row = cr.fetchOne();
            current = Utils.toInt(row[0]);
            String current_day = Utils.toString(row[1]);
            if (!Objects.equals(currentDate, current_day)) {
                current = start;
            }
        }
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
            current = current + (step * (i + 1));
            codes.add(code);
        }
        return codes;
    }
}
