package jmaa.modules.md.label.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.exceptions.ModelException;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

@Model.Meta(name = "lbl.material_label", inherit = "mixin.material", label = "物料条码", present = "sn")
@Model.Service(remove = "@edit")
public class MaterialLabel extends ValueModel {
    static Field lot_num = Field.Char().label("物料批次号");
    static Field sn = Field.Char().label("序列号").unique();
    static Field abc_type = Field.Selection().label("ABC分类").related("material_id.abc_type");
    static Field qty = Field.Float().label("数量");
    static Field product_lot = Field.Char().label("生产批次");
    static Field product_date = Field.Date().label("生产日期");
    static Field original_qty = Field.Float().label("原始数量").required().defaultValue(0D);
    static Field package_id = Field.Many2one("packing.package").label("包装号");

    /**
     * 创建标签，根据打印数量和最少包装数量计算标签个数，最后不满最小包装的为尾数
     */
    public Records createLabel(Records records, String materialId, double minPackages, double printQty, Date productDate,
                               String productLot, String lotNum, Map<String, Object> data) {
        if (Utils.lessOrEqual(minPackages, 0)) {
            throw new ValidationException("标签数量必须大于0");
        }
        if (Utils.lessOrEqual(printQty, 0)) {
            throw new ValidationException("打印数量必须大于0");
        }
        //打印标签个数
        int count = (int) Math.ceil(printQty / minPackages);
        if (count > 10000) {
            throw new ValidationException("单次标签打印不能超过1万个标签");
        }
        List<String> codes = createCodes(records, materialId, count);
        //条码明细
        List<Map<String, Object>> labels = new ArrayList<>();
        double printed = 0d;
        for (String sn : codes) {
            Map<String, Object> detail = new HashMap<>();
            printed = Utils.round(printed + minPackages);
            double qty = Utils.lessOrEqual(printed, printQty) ? minPackages : Utils.round(printQty - printed + minPackages);
            detail.put("sn", sn);
            detail.put("lot_num", lotNum);
            detail.put("material_id", materialId);
            detail.put("qty", qty);
            detail.put("original_qty", qty);
            detail.put("product_lot", productLot);
            detail.put("product_date", productDate);
            detail.putAll(data);
            labels.add(detail);
        }
        //条码重复导致插入失败时，重新生成条码再试
        return tryBatchSave(records, labels, materialId, 10);
    }

    public Records tryBatchSave(Records records, List<Map<String, Object>> labels, String materialId, int retry) {
        //重试次数
        try {
            retry--;
            return records.createBatch(labels);
        } catch (ModelException e) {
            if (retry > 0) {
                List<String> codes = createCodes(records, materialId, labels.size());
                for (int i = 0; i < labels.size(); i++) {
                    Map<String, Object> label = labels.get(i);
                    String sn = codes.get(i);
                    label.put("sn", sn);
                }
                return tryBatchSave(records, labels, materialId, retry);
            } else {
                throw e;
            }
        }
    }

    /**
     * 生成指定数量的条码
     *
     * @param records
     * @param materialId 可以为空
     * @param count
     * @return
     */
    public List<String> createCodes(Records records, String materialId, int count) {
        Records coding = records.getEnv().get("code.coding");
        if (Utils.isNotEmpty(materialId)) {
            //根据物料查找条码规则
            Records material = records.getEnv().get("md.material", materialId);
            coding = material.getRec("sn_coding_id");
        }
        if (!coding.any()) {
            coding = coding.find(Criteria.equal("code", "SYS-SN"));
        }
        return (List<String>) coding.call("createCodes", count, Collections.emptyMap());
    }
}
