package jmaa.modules.label.manager.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;
import org.jmaa.sdk.util.ServerDate;

import java.util.*;

/**
 * @author 梁荣振
 * 物料条码
 */
@Model.Meta(inherit = "lbl.material_label")
public class MaterialLabel extends Model {
    static Field material_id = Field.Many2one("md.material").label("物料").required(false);
    static Field stock_rule = Field.Selection().label("库存规则").related("material_id.stock_rule");
    static Field location_id = Field.Many2one("md.store_location").label("库位");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库");
    static Field quality_status = Field.Selection(new Options() {{
        put("ok", "合格");
        put("ng", "不合格");
        put("overdue", "超期");
        put("scrap", "报废");
        put("red", "红牌");
    }}).label("质量状态");
    static Field status = Field.Selection(new Options() {{
        put("new", "新建");
        put("received", "已收货");
        put("to-stock", "待入库");
        put("to-rtv", "待退货");
        put("onhand", "在库");
        put("checking", "盘点中");
        put("balance", "平账");
        put("allot", "已分配");
        put("stock-out", "出库");
        put("online", "在线");
        put("feed", "上料");
        put("unload", "下料");
        put("use-up", "用毕");
        put("to-check", "待清点");
        put("to-return", "待退料");
        put("return", "退料");
        put("red", "红牌");
        put("delete-initial", "取消初始化");
    }}).label("状态").defaultValue("new");
    static Field overdue_date = Field.Date().label("超期日期");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field print_times = Field.Integer().label("打印次数");
    static Field last_print_time = Field.DateTime().label("最后打印时间");
    static Field print_template_id = Field.Many2one("print.template").label("打印模板");
    static Field split_ids = Field.One2many("lbl.label_split", "old_label_id").label("拆分记录");
    static Field status_ids = Field.One2many("lbl.material_label_status", "label_id").label("状态记录");
    static Field label_print_id = Field.Many2one("lbl.label_print").label("打印记录");

    public void logStatus(Records records, Map<String, Object> values) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Records record : records) {
            Map<String, Object> value = new HashMap<>();
            value.putAll(values);
            value.put("label_id", record.getId());
            value.put("sn", record.get("sn"));
            value.put("lot_num", record.get("lot_num"));
            value.put("status", record.get("status"));
            value.put("material_id", record.getRec("material_id").getId());
            value.put("qty", record.get("qty"));
            value.put("quality_status", record.get("quality_status"));
            value.put("company_id", record.getEnv().getCompany().getId());
            data.add(value);
        }
        records.getEnv().get("lbl.material_label_status").createBatch(data);
    }

    @Constrains("product_date")
    public void updateOverDueDate(Records records) {
        for (Records rec : records) {
            Records material = rec.getRec("material_id");
            // 物品保质期
            Integer shelfLife = material.getInteger("shelf_life");
            if (Utils.large(shelfLife, 0)) {
                // 生产日期
                Date productionDate = rec.getDate("product_date");
                // 添加时间
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(productionDate);
                calendar.add(Calendar.DATE, shelfLife);
                // 设置过期时间
                rec.set("overdue_date", calendar.getTime());
            }
        }
    }

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
        String packageId = null;
        if (Utils.isNotEmpty(lpn)) {
            Records pkg = records.getEnv().get("packing.package").find(Criteria.equal("code", lpn));
            if (!pkg.any()) {
                throw new ValidationException(records.l10n("LPN[%s]不存在", lpn));
            }
            if (!Utils.equals(materialId, pkg.getRec("material_id").getId())) {
                throw new ValidationException(records.l10n("LPN[%s]物料[%s]与打印物料[%s]不一致",
                    lpn, pkg.getRec("material_id").get("code"), records.getEnv().get("md.material", materialId).get("code")));
            }
            double qty = pkg.getDouble("qty");
            double packageQty = pkg.getDouble("package_qty");
            if (Utils.large(qty + printQty, packageQty)) {
                throw new ValidationException(records.l10n("打印数量已超过LPN包装数量[%s]", packageQty));
            }
            pkg.set("qty", Utils.round(qty + printQty));
            packageId = pkg.getId();
        }
        Records material = records.getEnv().get("md.material", materialId);
        String stockRule = material.getString("stock_rule");
        Records printTemplate = records.getEnv().get("print.template", printTplId);
        String supplierId = (String) data.get("supplier_id");
        Records labelPrint = records.getEnv().get("lbl.label_print");
        Map<String, Object> labelPrintData = new HashMap<>();
        labelPrintData.put("material_id", materialId);
        labelPrintData.put("template_id", printTplId);
        labelPrintData.put("min_packages", minPackages);
        labelPrintData.put("print_qty", printQty);
        labelPrintData.put("product_date", productDate);
        labelPrintData.put("product_lot", productLot);
        labelPrintData.put("lot_attr", lotAttr);
        labelPrintData.put("supplier_id", data.get("supplier_id"));
        labelPrintData.put("customer_id", data.get("customer_id"));
        labelPrintData.put("lpn", lpn);
        if ("sn".equals(stockRule)) {
            Map<String, Object> labelData = new KvMap()
                .set("print_template_id", printTplId)
                .set("print_times", 1)
                .set("package_id", packageId)
                .set("last_print_time", new ServerDate());
            labelData.putAll(data);
            String lotNum = (String) records.getEnv().get("lbl.lot_num").call("getLotNum", materialId, productDate, lotAttr, supplierId);
            Records labels = (Records) records.call("createLabel", materialId, minPackages, printQty, productDate, productLot, lotNum, labelData);
            labelPrintData.put("lot_num", lotNum);
            labelPrintData.put("start_label_code", labels.first().get("sn"));
            labelPrintData.put("end_label_code", labels.browse(labels.getIds()[labels.size() - 1]).get("sn"));
            labelPrint = labelPrint.create(labelPrintData);
            labels.set("label_print_id", labelPrint.getId());
            return (Map<String, Object>) printTemplate.call("print", new KvMap().set("labels", labels));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        int count = (int) Math.ceil(printQty / minPackages);
        String lotNum = "";
        if ("lot".equals(stockRule)) {
            lotNum = (String) records.getEnv().get("lbl.lot_num").call("getLotNum", materialId, productDate, lotAttr, supplierId);
        }
        List<String> codes = (List<String>) records.call("createCodes", materialId, count);
        labelPrintData.put("lot_num", lotNum);
        labelPrintData.put("start_label_code", codes.get(0));
        labelPrintData.put("end_label_code", codes.get(codes.size() - 1));
        labelPrint.create(labelPrintData);
        double leftQty = printQty;
        for (String sn : codes) {
            double qty = Utils.lessOrEqual(leftQty, minPackages) ? leftQty : minPackages;
            leftQty = Utils.round(leftQty - minPackages);
            String code = sn + "|" + material.get("code") + "|";
            if ("lot".equals(stockRule)) {
                code += lotNum + "|";
            }
            code += qty;
            Map<String, Object> labelData = new KvMap()
                .set("code", code)
                .set("sn", sn)
                .set("lot_num", lotNum)
                .set("qty", qty)
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("unit", material.getRec("unit_id").get("name"))
                .set("product_date", Utils.format(productDate, "yyyy-MM-dd"))
                .set("product_lot", productLot)
                .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            if (Utils.isNotEmpty(supplierId)) {
                Records supplier = records.getEnv().get("md.supplier", (String) data.get("supplier_id"));
                labelData.put("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"));
                labelData.put("supplier_chars", supplier.get("chars"));
                labelData.put("supplier_code", supplier.get("code"));
            }
            list.add(labelData);
        }
        return (Map<String, Object>) printTemplate.call("print", new KvMap().set("data", list));
    }

    /**
     * 补打条码。 如有库存，更新标签数量
     *
     * @param records
     * @return
     */
    @Model.ServiceMethod(label = "标签补打")
    public Map<String, Object> reprintLabel(Records records) {
        if (!records.any()) {
            throw new ValidationException("请选择要打印的标签");
        }
        Records printTemplate = records.first().getRec("print_template_id");
        for (Records rec : records) {
            if (!Utils.equals(rec.getRec("print_template_id").getId(), printTemplate.getId())) {
                throw new ValidationException("补打的标签模板不一致，不能一起打印");
            }
        }
        updateLabel(records);
        if(records.getEnv().getRegistry().contains("stock.onhand")) {
            Records onhand = records.getEnv().get("stock.onhand");
            if (onhand.getMeta().isAuto()) {
                for (Records label : records) {
                    String sn = label.getString("sn");
                    onhand = onhand.find(Criteria.equal("sn", sn));
                    if (onhand.any()) {
                        label.set("qty", onhand.getDouble("usable_qty"));
                    }
                }
            }
        }
        return (Map<String, Object>) printTemplate.call("print", new KvMap() {{
            put("labels", records);
        }});
    }

    void updateLabel(Records records) {
        for (Records detail : records) {
            detail.set("print_times", Utils.toInt(detail.get("print_times")) + 1);
            detail.set("last_print_time", new ServerDate());
        }
    }

    /**
     * 在库标签拆分
     */
    public Records splitLabel(Records record, String sn, Double splitQty) {
        if (Utils.lessOrEqual(splitQty, 0)) {
            throw new ValidationException(record.l10n("拆分数量必须大于0"));
        }
        Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
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

    /**
     * 不库标签拆分
     */
    public Records splitLabelNotOnhand(Records record, String sn, Double splitQty) {
        if (Utils.lessOrEqual(splitQty, 0)) {
            throw new ValidationException(record.l10n("拆分数量必须大于0"));
        }
        Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", sn));
        if (!label.any()) {
            throw new ValidationException(record.l10n("物料标签[%s]不存在", sn));
        }
        label.set("qty", Utils.round(label.getDouble("qty") - splitQty));
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
        labelData.put("serial_number", sn);
        labelData.put("print_template_id", label.getRec("print_template_id").getId());
        labelData.put("print_times", 1);
        labelData.put("last_print_time", new ServerDate());
        toSave.add(labelData);
        Records newLabel = (Records) record.call("tryBatchSave", toSave, materialId, 10);
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "lbl.material_label");
        log.put("related_id", label.getId());
        log.put("related_code", label.get("sn"));
        newLabel.call("logStatus", log);
        log = new HashMap<>();
        log.put("operation", "lbl.material_label:split");
        log.put("related_id", newLabel.getId());
        log.put("related_code", code);
        label.call("logStatus", log);
        return newLabel;
    }


    /**
     * 打印空白标签，不绑定物料信息
     *
     * @param records
     * @param count
     * @param printTplId
     * @return
     */
    @Model.ServiceMethod(label = "空白标签打印")
    public Map<String, Object> printBlankLabel(Records records, int count, String printTplId) {
        //标签模板
        if (Utils.isEmpty(printTplId)) {
            throw new ValidationException(records.l10n("请选择标签模板"));
        }
        Records labels = createBlankLabel(records, count, printTplId);
        Records printTemplate = records.getEnv().get("print.template").browse(printTplId);
        Map<String, Object> printResult = (Map<String, Object>) printTemplate.call("print", new KvMap() {{
            put("labels", labels);
        }});
        return printResult;
    }

    /**
     * 创建空白标签
     */
    public Records createBlankLabel(Records records, int count, String printTplId) {
        List<String> codes = (List<String>) records.call("createCodes", null, count);
        List<Map<String, Object>> labels = new ArrayList<>();
        for (String code : codes) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("sn", code);
            detail.put("print_template_id", printTplId);
            detail.put("print_times", 1);
            detail.put("last_print_time", new ServerDate());
            labels.add(detail);
        }
        //条码重复导致插入失败时，重新生成条码再试
        return (Records) records.call("tryBatchSave", labels, null, 10);
    }

    @Model.ServiceMethod(label = "在库标签拆分")
    public Object printSplitLabel(Records record, String sn, Double splitQty, Boolean printOld) {
        Records newLabel = splitLabel(record, sn, splitQty);
        Records printTemplate = newLabel.getRec("print_template_id");
        List<String> labelIds = new ArrayList<>();
        if (printOld) {
            Records label = newLabel.find(Criteria.equal("sn", sn));
            labelIds.add(label.getId());
        }
        labelIds.add(newLabel.getId());
        Map<String, Object> printData = (Map<String, Object>) printTemplate.call("print", new KvMap() {{
            put("labels", newLabel.browse(labelIds));
        }});
        printData.put("newSn", newLabel.get("sn"));
        return printData;
    }

    @Model.ServiceMethod(label = "条码变更", auth = "read")
    public Object getSnQty(Records record, String sn) {
        Records label = record.find(Criteria.equal("sn", sn));
        if (!label.any()) {
            throw new ValidationException(record.l10n("物料标签[%s]不存在", sn));
        }
        return label.read(Arrays.asList("qty"));
    }
}
