package jmaa.modules.code.rule.models;

import org.jmaa.sdk.*;

import java.util.*;
/**
 * @author eric
 */
@Model.Meta(name = "code.part.month_sequence", description = "按月生成流水号算法，每月1号重新编码", label = "按月序列号", inherit = "code.part.global_sequence", logAccess = BoolState.False, authModel = "code.coding")

public class PartMonthSequence extends Model {

    static Field current_month = Field.Char().label("当前年月");

    /**
     * 生成编码
     *
     * @param rec
     * @param partId 编码组成类型ID
     * @return 编码
     */
    public List<String> getPartCode(Records rec, String partId, int qty) {
        //参考日流水处理并发问题
        throw new UnsupportedOperationException();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM");
//        //当前日期
//        String currentMonth = dateFormat.format(new Date());
//
//        Records partRecord = rec.getEnv().get("code.part", partId);
//        partRecord.ensureOne();
//        String content = (String) partRecord.get("content");
//        JSONObject jsonObject = JSONObject.parseObject(content);
//        Integer start = jsonObject.getInteger("start");
//        Integer step = jsonObject.getInteger("step");
//        Integer length = jsonObject.getInteger("length");
//
//        Records sequenceRecord = rec.find(Criteria.equal("code_part_id", partRecord.get("id")));
//        if (sequenceRecord.any()) {
//            sequenceRecord.ensureOne();
//            Integer current = (Integer) sequenceRecord.get("current_sequence");
//            String current_month = (String) sequenceRecord.get("current_month");
//
//            //如果日期不是上一次的月份，则重新开始计算
//            if (!Objects.equals(currentMonth, current_month)) {
//                current = start;
//            }
//
//            List<String> codes = new ArrayList<>();
//            for (int i = 0; i < qty; i++) {
//                String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
//                current = current + step;
//                codes.add(code);
//            }
//            try (Cursor cr = rec.getEnv().getDatabase().openCursor()) {
//                cr.execute("UPDATE code_part_month_sequence SET current_sequence=%s,current_month=%s WHERE id=%s",
//                        Arrays.asList(current, current_month, sequenceRecord.getId()));
//                cr.commit();
//            }
//
//            return Collections.singletonList(Utils.Strings.leftPad(String.valueOf(current + step), length, "0"));
//        } else {
//            int current = start;
//            List<String> codes = new ArrayList<>();
//            for (int i = 0; i < qty; i++) {
//                String code = Utils.Strings.leftPad(String.valueOf(current), length, "0");
//                current = current + step;
//                codes.add(code);
//            }
//            String id = IdWorker.nextId();
//            cr.execute("INSERT INTO code_part_day_sequence(id,code_part_id,current_day,current_sequence) values(%s,%s,%s,%s)",
//                    Arrays.asList(id, partId, currentDate, current));
//            cr.commit();
//            return codes;
//            rec.create(new KvMap()
//                    .set("code_part_id", partRecord.get("id"))
//                    .set("length", length)
//                    .set("step", step)
//                    .set("current_month", currentMonth)
//                    .set("current_sequence", current)
//            );
//
//            return Collections.singletonList(Utils.Strings.leftPad(String.valueOf(currentNum), length, "0"));
//        }
    }

    public String getDescription(Records rec) {
        return "按月份生成流水号算法，每月重新编码";
    }

}
