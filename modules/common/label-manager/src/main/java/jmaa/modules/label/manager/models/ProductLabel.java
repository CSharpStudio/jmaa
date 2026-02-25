package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;

@Model.Meta(name = "lbl.product_label", inherit = {"lbl.product_label", "lbl.material_label"})
public class ProductLabel extends Model {
    /**
     * 打印标签
     */
    @Model.ServiceMethod(label = "标签打印")
    public Map<String, Object> printLabel(Records records,
                                          @Doc("物料ID") String materialId,
                                          @Doc("标签数量") double minPackages,
                                          @Doc("打印数量") double printQty,
                                          @Doc("生产周期") Date productDate,
                                          @Doc("生产批次") String productLot,
                                          @Doc("批次属性") String lotAttr,
                                          @Doc("打印模板") String printTplId,
                                          @Doc("最外层包装") String lpn,
                                          @Doc("其它参数") Map<String, Object> data) {
        //标签模板
        if (Utils.isEmpty(printTplId)) {
            throw new ValidationException(records.l10n("请选择标签模板"));
        }
        Map<String, Object> labelData = new KvMap()
            .set("print_template_id", printTplId)
            .set("print_times", 1)
            .set("last_print_time", new ServerDate());
        labelData.putAll(data);
        String lotNum = (String) records.getEnv().get("lbl.lot_num").call("getLotNum", materialId, productDate, lotAttr, data.get("supplier_id"));
        Records labels = (Records) records.call("createLabel", materialId, minPackages, printQty, productDate, productLot, lotNum, labelData);
        Records printTemplate = records.getEnv().get("print.template").browse(printTplId);
        Map<String, Object> printResult = (Map<String, Object>) printTemplate.call("print", new KvMap() {{
            put("labels", labels);
        }});
        return printResult;
    }

    // 成品标签拆分
    public Records splitLabel(Records record, String sn, Double splitQty) {
        if (Utils.lessOrEqual(splitQty, 0)) {
            throw new ValidationException(record.l10n("拆分数量必须大于0"));
        }
        Records label = record.getEnv().get("lbl.product_label").find(Criteria.equal("sn", sn));
        if (!label.any()) {
            throw new ValidationException(record.l10n("物料标签[%s]不存在", sn));
        }
        Records onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("物料标签[%s]不在库", sn));
        }
        if (!"onhand".equals(onhand.get("status"))) {
            throw new ValidationException(record.l10n("物料标签[%s]状态为[%s]，不能拆分", sn, onhand.getSelection("status")));
        }
        Double onhandQty = onhand.getDouble("usable_qty");
        if (Utils.largeOrEqual(splitQty, onhandQty)) {
            throw new ValidationException(record.l10n("物料标签[%s]拆分数量必须小于在库可用数量[%s]", sn, onhandQty));
        }
        Cursor cr = record.getEnv().getCursor();
        cr.execute("update stock_onhand set usable_qty=usable_qty-%s,ok_qty=ok_qty-%s where id=%s", Arrays.asList(splitQty, splitQty, onhand.getId()));
        cr.execute("select ok_qty from stock_onhand where id=%s", Arrays.asList(onhand.getId()));
        label.set("qty", Utils.toDouble(cr.fetchOne()[0]));
        String materialId = label.getRec("material_id").getId();
        String code = ((List<String>) record.call("createCodes", materialId, 1)).get(0);
        List<Map<String, Object>> toSave = new ArrayList<>();
        Map<String, Object> labelData = new HashMap<>();
        labelData.put("sn", code);
        labelData.put("lot_num", label.get("lot_num"));
        labelData.put("material_id", materialId);
        labelData.put("qty", splitQty);
        labelData.put("status", "onhand");
        labelData.put("original_qty", label.get("original_qty"));
        labelData.put("product_lot", label.get("product_lot"));
        labelData.put("product_date", label.get("product_date"));
        labelData.put("quality_status", label.get("quality_status"));
        labelData.put("supplier_id", label.getRec("supplier_id").getId());
        labelData.put("customer_id", label.getRec("customer_id").getId());
        labelData.put("warehouse_id", onhand.getRec("warehouse_id").getId());
        labelData.put("location_id", onhand.getRec("location_id").getId());
        labelData.put("serial_number", sn);
        labelData.put("print_template_id", label.getRec("print_template_id").getId());
        labelData.put("print_times", 1);
        labelData.put("last_print_time", new ServerDate());
        labelData.put("work_order_id", label.getRec("work_order_id").getId());;
        toSave.add(labelData);
        Records newLabel = (Records) record.call("tryBatchSave", toSave, materialId, 10);
        Map<String, Object> onhandData = new HashMap<>();
        onhandData.put("material_id", materialId);
        onhandData.put("ok_qty", splitQty);
        onhandData.put("usable_qty", splitQty);
        onhandData.put("sn", code);
        onhandData.put("company_id", onhand.getRec("company_id").getId());
        onhandData.put("label_id", newLabel.getId());
        onhandData.put("warehouse_id", onhand.getRec("warehouse_id").getId());
        onhandData.put("location_id", onhand.getRec("location_id").getId());
        onhandData.put("stock_in_time", onhand.get("stock_in_time"));
        onhandData.put("status", "onhand");
        record.getEnv().get("stock.onhand").create(onhandData);
        //分别记录标签操作日志
        String location = onhand.getRec("location_id").getString("present");
        if (Utils.isEmpty(location)) {
            location = onhand.getRec("warehouse_id").getString("present");
        }
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "lbl.material_label");
        log.put("related_id", label.getId());
        log.put("related_code", label.get("sn"));
        log.put("location", location);
        newLabel.call("logStatus", log);
        log = new HashMap<>();
        log.put("operation", "lbl.material_label:split");
        log.put("related_id", newLabel.getId());
        log.put("related_code", code);
        log.put("location", location);
        label.call("logStatus", log);
        return newLabel;
    }
}
