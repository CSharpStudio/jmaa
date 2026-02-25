package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author eric
 */
@Model.Meta(name = "wms.return_supplier", label = "退供应商", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class ReturnSupplier extends Model {
    static Field code = Field.Char().label("退货单号").readonly().unique();
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商").required();
    static Field line_ids = Field.One2many("wms.return_supplier_line", "return_id").label("物料明细");
    static Field details_ids = Field.One2many("wms.return_supplier_details", "return_id").label("退料明细");
    static Field return_date = Field.Date().label("退货日期");
    static Field remark = Field.Char().label("备注");

    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
            Records returnSupplierLine = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()));
            if (!returnSupplierLine.any()) {
                throw new ValidationException("请先添加物料明细数据,再提交");
            }
            // 他可能来料流程也驳回再提交的话,这里需要控制是否在库? 控制不了好像,
            for (Records line : returnSupplierLine) {
                // 需退货数
                double requestQty = line.getDouble("request_qty");
                Records purchaseOrder = line.getRec("po_id");
                Records material = line.getRec("material_id");
                Records purchaseOrderLine = record.getEnv().get("purchase.order_line")
                    .find(Criteria.equal("po_id", purchaseOrder.getId()).and(Criteria.equal("material_id", material.getId())).and(Criteria.greater("receive_qty", 0)));
                if (!purchaseOrderLine.any()) {
                    throw new ValidationException(record.l10n("退货物料的无送货数据,不能退货"));
                }
                // 手动选择的物料,数量也是手动输入,可能,采购订单有多条相同物料数据,只是行号不同,这种情况下需要做数据处理,
                // 按照审核的时候添加退货数,这种情况处理不了,最后出库的时候,如何分配,
                double poRreturnedQty = purchaseOrderLine.stream().mapToDouble(e -> e.getDouble("return_qty")).sum();
                double receivedQty = purchaseOrderLine.stream().mapToDouble(e -> e.getDouble("receive_qty")).sum();
                if (Utils.large(Utils.round(poRreturnedQty + requestQty), receivedQty)) {
                    // 当前退货数+ 累计退货数 > 送货数,
                    throw new ValidationException(record.l10n("物料[%s]累计退货数(包含本次)超出送货数,不能退货", material.getString("code")));
                }
                for (Records poLine : purchaseOrderLine) {
                    double returnedQty = poLine.getDouble("return_qty");
                    double receiveQty = poLine.getDouble("receive_qty");
                    // 可以操作的数量
                    double operationQty = Utils.round(receiveQty - returnedQty);
                    if (Utils.lessOrEqual(requestQty, operationQty)) {
                        poLine.set("return_qty", Utils.round(returnedQty + requestQty));
                        break;
                    }
                    requestQty = Utils.round(requestQty - operationQty);
                    poLine.set("return_qty", Utils.round(returnedQty + operationQty));
                }
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "approve");
        return Action.reload(records.l10n("操作成功"));
    }

    @Model.ServiceMethod(label = "驳回", doc = "驳回单据，从提交或审核状态改为驳回")
    public Object reject(Records records, @Doc(doc = "驳回原因") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以驳回", record.getSelection("status")));
            }
            Records returnSupplierLine = record.getRec("line_ids");
            for (Records line : returnSupplierLine) {
                // 需退货数
                double requestQty = line.getDouble("request_qty");
                Records purchaseOrder = line.getRec("po_id");
                Records material = line.getRec("material_id");
                Records purchaseOrderLine = record.getEnv().get("purchase.order_line")
                    .find(Criteria.equal("po_id", purchaseOrder.getId()).and(Criteria.equal("material_id", material.getId()))
                        .and(Criteria.greater("receive_qty", 0)));
                for (Records poLine : purchaseOrderLine) {
                    double returnedQty = poLine.getDouble("return_qty");
                    if (Utils.lessOrEqual(requestQty, returnedQty)) {
                        poLine.set("return_qty", Utils.round(returnedQty - requestQty));
                        break;
                    }
                    requestQty = Utils.round(requestQty - returnedQty);
                    poLine.set("return_qty", 0d);
                }
            }
        }
        String body = records.l10n("驳回") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "reject");
        return Action.reload(records.l10n("操作成功"));
    }

    @ServiceMethod(label = "读取退货信息", auth = "returns", doc = "物料为空时读取第一个物料，物料不为空时读取下一个物料")
    public Map<String, Object> readReturnMaterial(Records record, @Doc("物料") String materialId) {
        Records lines = record.getEnv().get("wms.return_supplier_line");
        Cursor cr = record.getEnv().getCursor();
        if (Utils.isNotEmpty(materialId)) {
            String sql = "select material_id from wms_return_supplier_line where return_id=%s and status in %s and material_id > %s order by material_id";
            sql = cr.getSqlDialect().getPaging(sql, 1, 0);
            cr.execute(sql, Arrays.asList(record.getId(), Arrays.asList("new", "returning"), materialId));
            if (cr.getRowCount() > 0) {
                lines = lines.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("material_id", cr.fetchOne()[0])));
            }
        }
        if (!lines.any()) {
            String sql = "select material_id from wms_return_supplier_line where return_id=%s and status in %s order by material_id";
            sql = cr.getSqlDialect().getPaging(sql, 1, 0);
            cr.execute(sql, Arrays.asList(record.getId(), Arrays.asList("new", "returning")));
            if (cr.getRowCount() > 0) {
                lines = lines.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("material_id", cr.fetchOne()[0])));
            }
        }
        if (!lines.any()) {
            throw new ValidationException("没有待退物料");
        }
        return getReturnMaterial(record, lines);
    }

    public Map<String, Object> getReturnMaterial(Records record, Records lines) {
        Double requestQty = 0d;
        Double returnedQty = 0d;
        for (Records line : lines) {
            requestQty += line.getDouble("request_qty");
            returnedQty += line.getDouble("return_qty");
        }
        Double deficitQty = Math.max(0, Utils.round(requestQty - returnedQty));
        Records material = lines.first().getRec("material_id");
        if (Utils.equals(0d, deficitQty)) {
            throw new ValidationException(record.l10n("当前物料[%s]已备齐，请核对物料明细", material.getString("present")));
        }
        Records unit = material.getRec("unit_id");
        Map<String, Object> data = new HashMap<>();
        data.put("material_id", material.getPresent());
        data.put("request_qty", requestQty);
        data.put("deficit_qty", deficitQty);
        data.put("material_name_spec", material.get("name_spec"));
        data.put("material_category", material.get("category"));
        data.put("stock_rule", material.get("stock_rule"));
        data.put("unit_id", unit.getPresent());
        data.put("unit_accuracy", unit.getInteger("accuracy"));
        return data;
    }

    @ServiceMethod(label = "扫描条码", doc = "物料编码则查询退货需求，物料标签则退货", auth = "returns")
    public Object scanCode(Records record, @Doc("标签条码/物料编码") String code) {
        Environment env = record.getEnv();

        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            if (!material.any()) {
                throw new ValidationException(record.l10n("物料[%s]不存在", codes[1]));
            }
            Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", material.getId()));
            if (!lines.any()) {
                throw new ValidationException(lines.l10n("物料[%s]不在退货清单", material.get("code")));
            }
            Records unit = material.getRec("unit_id");
            Map<String, Object> returnMaterial = getReturnMaterial(record, lines);
            // 剩余需退货数
            double deficitQty = Utils.toDouble(returnMaterial.get("deficit_qty"));
            // 获取仓库,库位数据
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                return getSnLabelReturnMap(record, code, codes, returnMaterial, deficitQty, unit);
            } else if ("lot".equals(stockRule)) {
                // lot_num-1|materialId|lot_num|qty
                if (env.getConfig().getBoolean("lot_in_qty")) {
                    Records lotSnTransient = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                        .and(Criteria.equal("type", "wms.return_supplier"))
                        .and(Criteria.equal("lot_num", codes[2]))
                        .and(Criteria.equal("material_id",material.getId()))
                        .and(Criteria.equal("sn", codes[0])));
                    if (lotSnTransient.any()) {
                        // 存在,
                        throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", codes[0]));
                    }
                }
                return getLotLabelReturnMap(record, code, codes, returnMaterial, deficitQty,material);
            } else {
                // num
                return getNumLabelReturnMap(record, code, codes, returnMaterial, deficitQty, material);
            }
        } else {
            // 当做成品标签
            Records productLabel = env.get("lbl.product_label").find(Criteria.equal("sn", code));
            if (productLabel.any()){
                Records material = productLabel.getRec("material_id");
                Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", material.getId()));
                if (!lines.any()) {
                    throw new ValidationException(lines.l10n("物料[%s]不在退货清单", material.get("code")));
                }
                Records unit = material.getRec("unit_id");
                Map<String, Object> returnMaterial = getReturnMaterial(record, lines);
                double deficitQty = Utils.toDouble(returnMaterial.get("deficit_qty"));
                return getProductLabelReturnMap(record, productLabel, returnMaterial, deficitQty, unit);
            }
            return getMaterialReturnMap(record, code);
        }
    }
    public Map<String, Object> getProductLabelReturnMap(Records record, Records productLabel, Map<String, Object> returnMaterial, double deficitQty, Records unit) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();

        Records details = env.get("wms.return_supplier_details").find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("label_id", productLabel.getId())));
        if (details.any()) {
            throw new ValidationException(record.l10n("标签[%s]已使用,请检查数据", code));
        }
        double labelQty = productLabel.getDouble("qty");
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", productLabel.getId()));
        if (onhand.any()) {
            returnMaterial.put("warehouse_id", onhand.getRec("warehouse_id").getId());
            returnMaterial.put("location_id", onhand.getRec("location_id").getId());
            returnMaterial.put("qty", onhand.getDouble("usable_qty"));
        } else {
            returnMaterial.put("qty", labelQty);
        }
        // 是否需要拆分
        if (Utils.largeOrEqual(deficitQty, labelQty)) {
            returnMaterial.put("commit_qty", labelQty);
            result.put("action", "material");
            result.put("data", returnMaterial);
            result.put("message", record.l10n("条码[%s]扫码成功", productLabel.get("sn")));
        } else {
            returnMaterial.put("commit_qty", deficitQty);
            Map<String, Object> data = new HashMap<>();
            data.put("accuracy", unit.getInteger("accuracy"));
            data.put("sn", productLabel.get("sn"));
            data.put("qty", labelQty);
            data.put("deficit_qty", deficitQty);
            data.put("split_qty", deficitQty);
            data.put("print_label", true);
            result.put("action", "split");
            result.put("data", returnMaterial);
            result.put("split", data);
            result.put("message", record.l10n("条码[%s]需要拆分", productLabel.get("sn")));
        }
        return result;
    }

    public Map<String, Object> getNumLabelReturnMap(Records record, String code, String[] codes, Map<String, Object> returnMaterial, double deficitQty, Records material) {
        Map<String, Object> result = new HashMap<>();
        // 数量管控那就随便了, 只管当前标签的数量,需要修改数量就随意
        Environment env = record.getEnv();
        double labelQty = Utils.toDouble(codes[codes.length - 1]);
        returnMaterial.put("qty", labelQty);
        returnMaterial.put("commit_qty", Utils.large(labelQty, deficitQty) ? deficitQty : labelQty);
        // 查询在库信息
        Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("material_id", material.getId())
            .and(Criteria.equal("label_id", null)).and(Criteria.equal("lot_num", null))).first();
        if (stockOnhand.any()) {
            stockOnhand = stockOnhand.first();
            returnMaterial.put("warehouse_id", stockOnhand.getRec("warehouse_id").getId());
            returnMaterial.put("location_id", stockOnhand.getRec("location_id").getId());
        }
        result.put("action", "material");
        result.put("data", returnMaterial);
        result.put("message", record.l10n("条码[%s]扫码成功", code));
        return result;
    }

    public Map<String, Object> getLotLabelReturnMap(Records record, String code, String[] codes, Map<String, Object> returnMaterial, double deficitQty,Records material) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        String lotNumCode = codes[2];
        double labelQty = 0d;
        // 先看看这个批次号是否存在
        Records lotNum = env.get("lbl.lot_num").find(Criteria.equal("code", lotNumCode).and(Criteria.equal("material_id",material.getId())));
        if (!lotNum.any()) {
            throw new ValidationException(record.l10n("当前批次标签无法识别,请检查数据"));
        }
        // 查一下这个批次是否在库
        Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("lot_num", lotNumCode).and(Criteria.equal("material_id",material.getId())));
        if (stockOnhand.any()) {
            stockOnhand = stockOnhand.first();
            returnMaterial.put("warehouse_id", stockOnhand.getRec("warehouse_id").getId());
            returnMaterial.put("location_id", stockOnhand.getRec("location_id").getId());
            labelQty = stockOnhand.getDouble("usable_qty");
        }
        boolean lotOutQtyFlag = env.getConfig().getBoolean("lot_out_qty");
        if (lotOutQtyFlag) {
            labelQty = Utils.toDouble(codes[codes.length - 1]);
        }
        returnMaterial.put("qty", labelQty);
        returnMaterial.put("commit_qty", Utils.large(labelQty, deficitQty) ? deficitQty : labelQty);
        returnMaterial.put("lot_out_qty", lotOutQtyFlag);
        result.put("action", "material");
        result.put("data", returnMaterial);
        result.put("message", record.l10n("条码[%s]扫码成功", code));
        return result;
    }

    @ServiceMethod(label = "扫描以后,确定", auth = "returns")
    public Object submitScanCode(Records record, String materialId, String warehouseId, String locationId, String sn, Double qty) {
        // 1. 要明确,前一步扫码以后,没有存明细表, 要在这里新增
        // 2. 获取的标签数据,sn lot num 都有,
        // 3. sn 可能会拆标签,  使用的标签要改为已分配状态
        // 4. 这里只做保存,退货的时候才控制库存数量
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        // 扫码的时候已经处理好了,
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", sn);
        StringBuilder packagingReturnMsg = new StringBuilder();
        Records returnSupplierLine = checkSupplierLine(record, materialId, qty,packagingReturnMsg);
        Records material = env.get("md.material", materialId);
        String stockRule = material.getString("stock_rule");
        selectStockRuleAndCreateReturnDetails(record, materialId, warehouseId, locationId, qty, stockRule, codes, env);
        // 回写物料明细数量,不然会一直扫
        // 他可能有多条相同物料数据吗, 不考虑
        writeBackSupplierLineStatus(qty, returnSupplierLine);
        result.put("message", record.l10n(packagingReturnMsg.toString()));
        return result;
    }



    public void selectStockRuleAndCreateReturnDetails(Records record, String materialId, String warehouseId, String locationId, Double qty, String stockRule, String[] codes, Environment env) {
        if ("sn".equals(stockRule)) {
            // 序列号
            String snCode = codes[0];
            // 创建明细数据
            Records materialLabel = env.get("lbl.material_label").find(Criteria.equal("sn", snCode));
            materialLabel.set("status", "allot");
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.return_supplier");
            log.put("related_id", record.getId());
            log.put("related_code", record.getString("code"));
            materialLabel.call("logStatus", log);
            if (Utils.isNotBlank(warehouseId)) {
                // 库存数据,
                Criteria criteria = Criteria.equal("label_id.sn", snCode).and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId));
                Records stockOnhand = env.get("stock.onhand").find(criteria);
                checkStockOnhandQty(record, qty, stockOnhand);
                // 序列号标签直接改
                stockOnhand.set("status", "allot");
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") + materialLabel.getDouble("qty")));
                stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") - materialLabel.getDouble("qty")));
            }
            createReturnDetailsBySubmitScanCode(record, qty, materialId, materialLabel.getId(), warehouseId, locationId, materialLabel.getString("lot_num"));
        } else if ("lot".equals(stockRule)) {
            String lotNumCode = codes[2];
            String sn = codes[0];
            if (env.getConfig().getBoolean("lot_in_qty")) {
                // 扫码已经确认是否使用了,这里不判断
                // 直接新增到临时表
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", record.getId());
                data.put("sn", sn);
                data.put("material_id", materialId);
                data.put("lot_num", lotNumCode);
                data.put("type", "wms.return_supplier");
                env.get("lbl.lot_status").create(data);
            }
            if (Utils.isNotBlank(warehouseId)) {
                Criteria criteria = Criteria.equal("lot_num", lotNumCode).and(Criteria.equal("material_id",materialId)).and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId));
                Records stockOnhand = env.get("stock.onhand").find(criteria);
                checkStockOnhandQty(record, qty, stockOnhand);
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") + qty));
                stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") - qty));
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d)) {
                    stockOnhand.set("status", "allot");
                }
            }
            createReturnDetailsBySubmitScanCode(record, qty, materialId, null, warehouseId, locationId, lotNumCode);
        } else {
            if (Utils.isNotBlank(warehouseId)) {
                Criteria criteria = Criteria.equal("material_id", materialId).and(Criteria.equal("lot_num", null)).and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId));
                Records stockOnhand = env.get("stock.onhand").find(criteria);
                checkStockOnhandQty(record, qty, stockOnhand);
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") + qty));
                stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") - qty));
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d)) {
                    stockOnhand.set("status", "allot");
                }
            }
            createReturnDetailsBySubmitScanCode(record, qty, materialId, null, warehouseId, locationId, null);
        }
    }

    public void checkStockOnhandQty(Records record, Double qty, Records stockOnhand) {
        if (!stockOnhand.any()) {
            throw new ValidationException(record.l10n("选择的仓库和库位,无对应库存数据,请检查数据"));
        }
        if (Utils.less(stockOnhand.getDouble("usable_qty"), qty)) {
            throw new ValidationException(record.l10n("输入的退货数量大于库存可用量,请检查数据"));
        }
    }

    public void writeBackSupplierLineStatus(Double qty, Records returnSupplierLine) {
        for (Records line : returnSupplierLine) {
            double requestQty = line.getDouble("request_qty");
            double returnedQty = line.getDouble("return_qty");
            if (Utils.large(requestQty, returnedQty) && !Utils.equals(qty, 0d)) {
                // 还有没有确定的数量
                double roundQty = Utils.round(requestQty - returnedQty);
                if (Utils.large(qty, roundQty)) {
                    // 标签数量大于待退
                    line.set("return_qty", line.getDouble("request_qty"));
                    qty = Utils.round(qty - roundQty);
                } else {
                    // 标签数量小于待退
                    line.set("return_qty", Utils.round(line.getDouble("return_qty") + qty));
                    qty = 0d;
                }
            }
        }
    }

    public Records checkSupplierLine(Records record, String materialId, Double qty,StringBuilder returnMsg) {
        Records returnSupplierLine = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", materialId));
        Double requestQty = 0d;
        Double returnedQty = 0d;
        for (Records line : returnSupplierLine) {
            requestQty += line.getDouble("request_qty");
            returnedQty += line.getDouble("return_qty");
        }
        Double deficitQty = Math.max(0, Utils.round(requestQty - returnedQty));
        if (Utils.large(qty, deficitQty)) {
            throw new ValidationException("退货数量大于待退数量,请检查数据");
        }
        String materialCode = returnSupplierLine.getRec("material_id").getString("code");
        returnMsg.append("物料[").append(materialCode).append("],需退数:").append(requestQty).append(",已退数:").append(Utils.round(returnedQty+qty));
        return returnSupplierLine;
    }

    public void createReturnDetailsBySubmitScanCode(Records record, Double qty, String materialId, String labelId, String warehouseId, String locationId, String lotNumCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("return_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        data.put("lot_num", lotNumCode);
        record.getEnv().get("wms.return_supplier_details").create(data);
    }

    public Map<String, Object> getMaterialReturnMap(Records record, String code) {
        Map<String, Object> result = new HashMap<>();
        Records material = record.getEnv().get("md.material").find(Criteria.equal("code", code));
        if (!material.any()) {
            throw new ValidationException(record.l10n("条码[%s]无法识别,请检查标签数据",code));
        }
        Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", material.getId()));
        if (!lines.any()) {
            throw new ValidationException(lines.l10n("物料[%s]不在退货清单", material.get("code")));
        }
        Map<String, Object> data = getReturnMaterial(record, lines);
        result.put("action", "material");
        result.put("data", data);
        result.put("message", lines.l10n("条码[%s]读取物料成功", code));
        return result;
    }

    public Map<String, Object> getSnLabelReturnMap(Records record, String code, String[] codes, Map<String, Object> returnMaterial, double deficitQty, Records unit) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
        if (!label.any()) {
            throw new ValidationException(record.l10n("当前标签不存在,请检查数据"));
        }
        Records details = env.get("wms.return_supplier_details").find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("label_id", label.getId())));
        if (details.any()) {
            throw new ValidationException(record.l10n("标签[%s]已使用,请检查数据", code));
        }
        double labelQty = label.getDouble("qty");
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (onhand.any()) {
            returnMaterial.put("warehouse_id", onhand.getRec("warehouse_id").getId());
            returnMaterial.put("location_id", onhand.getRec("location_id").getId());
            returnMaterial.put("qty", onhand.getDouble("usable_qty"));
        } else {
            returnMaterial.put("qty", labelQty);
        }
        // 是否需要拆分
        if (Utils.largeOrEqual(deficitQty, labelQty)) {
            returnMaterial.put("commit_qty", labelQty);
            result.put("action", "material");
            result.put("data", returnMaterial);
            result.put("message", record.l10n("条码[%s]扫码成功", label.get("sn")));
        } else {
            returnMaterial.put("commit_qty", deficitQty);
            Map<String, Object> data = new HashMap<>();
            data.put("accuracy", unit.getInteger("accuracy"));
            data.put("sn", label.get("sn"));
            data.put("qty", labelQty);
            data.put("deficit_qty", deficitQty);
            data.put("split_qty", deficitQty);
            data.put("print_label", true);
            result.put("action", "split");
            result.put("data", returnMaterial);
            result.put("split", data);
            result.put("message", record.l10n("条码[%s]需要拆分", label.get("sn")));
        }
        return result;
    }

    @ServiceMethod(label = "提交条码", doc = "扫码以后,确定", auth = "returns")
    public Object submitCode(Records record, @Doc("标签条码/物料编码") String code) {
        Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", code));
        Records material = null;
        if (label.any()) {
            material = label.getRec("material_id");
            if ("sn".equals(material.get("stock_rule"))) {
                return submitMaterialLabel(record, label);
            }
        } else {
            material = record.getEnv().get("md.material").find(Criteria.equal("code", code));
            if (!material.any()) {
                throw new ValidationException(record.l10n("条码[%s]无法识别", code));
            }
        }
        Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", material.getId()));
        if (!lines.any()) {
            throw new ValidationException(lines.l10n("物料[%s]不在退货清单", material.get("code")));
        }
        Map<String, Object> data = getReturnMaterial(record, lines);
        Map<String, Object> result = new HashMap<>();
        result.put("action", "material");
        result.put("data", data);
        result.put("message", lines.l10n("条码[%s]读取物料成功", code));
        return result;
    }

    public Object submitMaterialLabel(Records record, Records label) {
        Records material = label.getRec("material_id");
        Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", material.getId()));
        if (!lines.any()) {
            throw new ValidationException(record.l10n("标签[%s]物料[%s]不在退货清单", label.get("sn"), material.get("present")));
        }
        Cursor cr = record.getEnv().getCursor();
        //事务锁定发料行
        cr.execute("update wms_return_supplier_line set id=id where id in %s", Arrays.asList(Arrays.asList(lines.getIds())));
        Double requestQty = 0d;
        Double returnedQty = 0d;
        for (Records line : lines) {
            requestQty += line.getDouble("request_qty");
            returnedQty += line.getDouble("return_qty");
        }
        Double deficitQty = Utils.round(requestQty - returnedQty);
        if (Utils.lessOrEqual(deficitQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        //查询来料入库明细。将入库明细标记为退货
        Records stockInDetails = record.getEnv().get("stock.stock_in_details").find(Criteria.equal("label_id", label.getId()));
        Map<String, Object> result = new HashMap<>();
        Records onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (!onhand.any()) {
            Records details = record.getEnv().get("wms.return_supplier_details").find(Criteria.equal("label_id", label.getId()));
            if (details.any()) {
                throw new ValidationException(record.l10n("物料标签[%s]已退货", label.get("sn")));
            }
            if (!stockInDetails.any()) {
                throw new ValidationException(record.l10n("物料标签[%s]不在库", label.get("sn")));
            }
            stockInDetails.set("status", "return");
            createReturnDetails(record, stockInDetails.getDouble("qty"), stockInDetails.getRec("material_id").getId(), stockInDetails.getRec("label_id").getId(), stockInDetails.getRec("warehouse_id").getId());
            return getScanResult(stockInDetails.getDouble("qty"), record, lines, result, label);
        }
        String onhandStatus = onhand.getString("status");
        if (!"onhand".equals(onhandStatus)) {
            throw new ValidationException(record.l10n("物料标签[%s]状态为[%s]", label.get("sn"), onhand.getSelection("status")));
        }
        Double onhandQty = onhand.getDouble("usable_qty");
        if (Utils.lessOrEqual(onhandQty, deficitQty)) {
            stockInDetails.set("status", "return");
            createReturnDetails(record, onhand.getDouble("usable_qty"), onhand.getRec("material_id").getId(), onhand.getRec("label_id").getId(), onhand.getRec("warehouse_id").getId());
            onhand.set("allot_qty", onhandQty);
            onhand.set("status", "allot");
            label.set("status", "allot");
            return getScanResult(onhandQty, record, lines, result, label);
        } else {
            Records unit = material.getRec("unit_id");
            Map<String, Object> data = new HashMap<>();
            data.put("accuracy", unit.getInteger("accuracy"));
            data.put("sn", label.get("sn"));
            data.put("qty", onhandQty);
            data.put("deficit_qty", deficitQty);
            data.put("split_qty", deficitQty);
            data.put("print_label", true);
            result.put("action", "split");
            result.put("data", getReturnMaterial(record, lines));
            result.put("split", data);
            result.put("message", record.l10n("条码[%s]需要拆分", label.get("sn")));
        }
        return result;
    }

    public Map<String, Object> getScanResult(Double qty, Records record, Records lines, Map<String, Object> result, Records label) {
        for (Records line : lines) {
            Double reqQty = line.getDouble("request_qty");
            Double retQty = line.getDouble("return_qty");
            if (Utils.largeOrEqual(retQty, reqQty)) {
                continue;
            }
            Double leftQty = Utils.round(reqQty - retQty);
            if (Utils.lessOrEqual(qty, leftQty)) {
                line.set("return_qty", retQty + qty);
                break;
            }
            qty = Utils.round(qty - leftQty);
            line.set("return_qty", reqQty);
        }
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "wms.return_supplier");
        log.put("related_id", record.getId());
        log.put("related_code", record.get("code"));
        label.call("logStatus", log);
        result.put("action", "material");
        result.put("data", getReturnMaterial(record, lines));
        result.put("message", record.l10n("条码[%s]退货成功", label.get("sn")));
        return result;
    }

    public void createReturnDetails(Records record, Double qty, String materialId, String labelId, String warehouseId) {
        Map<String, Object> data = new HashMap<>();
        data.put("return_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        record.getEnv().get("wms.return_supplier_details").create(data);
    }

    @ServiceMethod(label = "拆分标签", doc = "拆分生成新标签")
    public Object splitLabel(Records record, @Doc("条码") String sn, @Doc("拆分数量") Double splitQty, @Doc("是否打印原标签") Boolean printOld) {
        // 在库标签拆分
        // 查一下是否在库
        Environment env = record.getEnv();
        Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("label_id.sn", sn));
        Records newLabel = null;
        if (stockOnhand.any()) {
            newLabel = (Records) record.getEnv().get("lbl.material_label").call("splitLabel", sn, splitQty);
        } else {
            // 不在库标签拆分
            newLabel = (Records) record.getEnv().get("lbl.material_label").call("splitLabelNotOnhand", sn, splitQty);
        }
        Records printTemplate = newLabel.getRec("print_template_id");
        List<String> labelIds = new ArrayList<>();
        labelIds.add(newLabel.getId());
        if (Utils.toBoolean(printOld)) {
            Records label = newLabel.find(Criteria.equal("sn", sn));
            labelIds.add(label.getId());
        }
        Records finalNewLabel = newLabel;
        Map<String, Object> printData = (Map<String, Object>) printTemplate.call("print", new HashMap<String, Object>() {{
            put("labels", finalNewLabel.browse(labelIds));
        }});
        printData.put("newSn", newLabel.get("sn"));
        printData.put("newQty", newLabel.get("qty"));
        return printData;
    }

    @Model.ServiceMethod(label = "退货")
    public Object returns(Records record, @Doc("仓库") String warehouseId, @Doc("物料") String materialId, @Doc("发料数量") Double qty, @Doc("标签") String sn) {
        if (Utils.lessOrEqual(qty, 0)) {
            throw new ValidationException(record.l10n("退货数量必须大于0"));
        }
        // 挑选退货,会生成对应的退货明细,不需要自己扫码,就算扫码,也扫不上,这里应该没用了,
        if (!"wms.pick_out".equals(record.getString("type")) && Utils.isBlank(warehouseId)) {
            throw new ValidationException("非挑选退货类型单据,仓库必填,请检查数据");
        }
        Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "=", materialId));
        if (!lines.any()) {
            throw new ValidationException("找不到退货物料");
        }
        Cursor cr = record.getEnv().getCursor();
        //事务锁定发料行
        cr.execute("update wms_return_supplier_line set id=id where id in %s", Arrays.asList(Arrays.asList(lines.getIds())));
        Records material = record.getEnv().get("md.material", materialId);
        if (!"num".equals(material.get("stock_rule"))) {
            throw new ValidationException(record.l10n("物料[%s]库存规则[%s]，请扫描标签发料", material.get("code"), material.getSelection("stock_rule")));
        }
        Double requestQty = 0d;
        Double returnedQty = 0d;
        for (Records line : lines) {
            requestQty += line.getDouble("request_qty");
            returnedQty += line.getDouble("return_qty");
        }
        Double toReturnQty = Utils.round(requestQty - returnedQty);
        if (Utils.lessOrEqual(toReturnQty, 0)) {
            throw new ValidationException(record.l10n("物料[%s]已满足需求", material.get("code")));
        }
        if (Utils.large(qty, toReturnQty)) {
            throw new ValidationException(record.l10n("退货数量[%s]不能大于待退数量[%s]", qty, toReturnQty));
        }
        Records onhand = record.getEnv().get("stock.onhand").find(Criteria.equal("material_id", materialId).and("warehouse_id", "=", warehouseId));
        if (!onhand.any()) {
            throw new ValidationException(record.l10n("仓库[%s]没有库存", record.getEnv().get("md.warehouse", warehouseId)));
        }
        createReturnDetails(record, qty, materialId, null, warehouseId);
        for (Records line : lines) {
            Double reqQty = line.getDouble("request_qty");
            Double retQty = line.getDouble("return_qty");
            if (Utils.largeOrEqual(retQty, reqQty)) {
                continue;
            }
            Double leftQty = Utils.round(reqQty - retQty);
            if (Utils.lessOrEqual(qty, leftQty)) {
                line.set("return_qty", retQty + qty);
                break;
            }
            qty = Utils.round(qty - leftQty);
            line.set("return_qty", reqQty);
        }
        cr.execute("update stock_onhand set allot_qty=allot_qty+%s where material_id=%s and warehouse_id=%s", Arrays.asList(qty, materialId, warehouseId));
        //在更新事务内校验分配数，确保没有负可用数
        cr.execute("select usable_qty,allot_qty from stock_onhand where material_id=%s and warehouse_id=%s", Arrays.asList(materialId, warehouseId));
        Object[] row = cr.fetchOne();
        if (Utils.less(Utils.toDouble(row[0]), Utils.toDouble(row[1]))) {
            throw new ValidationException(record.l10n("退货数量[%s]不能大于库存可用数量[%s]", qty, Utils.round(Utils.toDouble(row[0]) - Utils.toDouble(row[1]))));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("data", getReturnMaterial(record, lines));
        result.put("message", record.l10n("退货成功"));
        return result;
    }

    @ServiceMethod(label = "出库", doc = "根据退货明细生成出库单")
    public Object stockOut(Records records, String comment) {
        for (Records record : records) {
            Records details = record.getEnv().get("wms.return_supplier_details").find(Criteria.equal("return_id", record.getId()).and("stock_out_id", "=", null));
            if (!details.any()) {
                throw new ValidationException("没有可出库物料");
            }
            // 就算审核的时候校验库存,实际出库的间隙, 还是会有使用库存, 那么这里需要校验库存数据, 如果库存不够,那就删除重新扫码,
            // 如果是未入库的数据,需要关联送货单吗,
            List<String> collect = details.stream().map(e -> e.getRec("material_id").getId()).collect(Collectors.toList());
            Records lines = record.getEnv().get("wms.return_supplier_line").find(Criteria.equal("return_id", record.getId()).and("material_id", "in", collect));
            List<String> poIds = getPoIdListAndSetLineStatus(lines);
            checkAndSetPoQty(record, details, poIds);
            // 处理退供应商物料明细状态
            //创建出库单
            Records stockOut = createStockOut(record, details);
            updateOnhandQty(details);
            //记录消息
            String body = records.l10n("生成出库单") + (Utils.isEmpty(comment) ? "" : ": " + comment);
            record.call("trackMessage", body);
            record.getEnv().get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.return_supplier"))).delete();
        }
        updateReturnSupplierStatus(records);
        return Action.reload(records.l10n("出库成功"));
    }

    public void updateOnhandQty(Records details) {
        Environment env = details.getEnv();
        for (Records detail : details) {
            Records material = detail.getRec("material_id");
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                // 序列号
                Records materialLabel = detail.getRec("label_id");
                materialLabel.set("status", "allot");
                // 库存数据,
                Records stockOnhand = env.get("stock.onhand").find(Criteria.equal("label_id", materialLabel.getId()));
                stockOnhand.delete();
            } else if ("lot".equals(stockRule)) {
                String lotNumCode = detail.getString("lot_num");
                double qty = detail.getDouble("qty");
                Criteria criteria = Criteria.equal("lot_num", lotNumCode).and(Criteria.equal("material_id",material.getId())).and(Criteria.equal("warehouse_id", detail.getRec("warehouse_id").getId())).and(Criteria.equal("location_id", detail.getRec("location_id").getId()));
                Records stockOnhand = env.get("stock.onhand").find(criteria);
                // 分配数减掉. 可用数减掉, ok数 减掉,
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d) && Utils.equals(stockOnhand.getDouble("allot_qty"), stockOnhand.getDouble("ok_qty"))) {
                    stockOnhand.delete();
                } else {
                    stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                    stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") - qty));
                }
            } else {
                double qty = detail.getDouble("qty");
                Criteria criteria = Criteria.equal("material_id", material.getId()).and(Criteria.equal("lot_num", null))
                    .and(Criteria.equal("warehouse_id", detail.getRec("warehouse_id").getId()))
                    .and(Criteria.equal("location_id", detail.getRec("location_id").getId()));
                Records stockOnhand = env.get("stock.onhand").find(criteria);
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d) && Utils.equals(stockOnhand.getDouble("allot_qty"), stockOnhand.getDouble("ok_qty"))) {
                    stockOnhand.delete();
                } else {
                    stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                    stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") - qty));
                }
            }
        }
    }

    public Records createStockOut(Records record, Records details) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "return-supplier");
        data.put("related_code", record.get("code"));
        data.put("related_model", record.getMeta().getName());
        data.put("related_id", record.getId());
        Records stockOut = record.getEnv().get("stock.stock_out").create(data);
        details.set("stock_out_id", stockOut.getId());
        stockOut.call("stockOut");
        return stockOut;
    }

    public void checkAndSetPoQty(Records record, Records details, List<String> poIds) {
        for (Records detail : details) {
            Records warehouse = detail.getRec("warehouse_id");
            if (!warehouse.any()) {
                // 审核的事实处理了,退货数, 没仓库也没有入库数,不需要处理
                continue;
            }
            Records material = detail.getRec("material_id");
            double qty = detail.getDouble("qty");
            Records poLines = record.getEnv().get("purchase.order_line").find(Criteria.in("po_id", poIds)
                .and(Criteria.equal("material_id", material.getId())).and(Criteria.greater("receive_qty", 0))
                .and(Criteria.greater("stock_in_qty", 0)));

            for (Records line : poLines) {
                Double stockInQty = line.getDouble("stock_in_qty");
                if (Utils.lessOrEqual(qty, stockInQty)) {
                    line.set("stock_in_qty", Utils.round(stockInQty - qty));
                    break;
                }
                qty = Utils.round(qty - stockInQty);
                line.set("stock_in_qty", 0d);
            }
        }
    }

    public List<String> getPoIdListAndSetLineStatus(Records lines) {
        List<String> poIds = new ArrayList<>();
        for (Records line : lines) {
            if (Utils.equals(line.getDouble("request_qty"), line.getDouble("return_qty"))) {
                line.set("status", "done");
            }
            Records po = line.getRec("po_id");
            if (po.any()) {
                poIds.add(po.getId());
            }
        }
        return poIds;
    }

    public Map<String, Double> getMaterialQtyGroup(Records record, Records details) {
        Map<String, Double> materialQty = new HashMap<>();
        for (Records detail : details) {
            Records material = detail.getRec("material_id");
            String materialId = material.getId();
            String stockRule = material.getString("stock_rule");
            Records warehouse = detail.getRec("warehouse_id");
            // 校验/扣减库存
            checkWarehouseStockOnhand(record, detail, warehouse, stockRule, materialId, material);
            Double qty = detail.getDouble("qty");
            Double mQty = materialQty.get(materialId);
            if (mQty == null) {
                materialQty.put(materialId, qty);
            } else {
                materialQty.put(materialId, Utils.round(mQty + qty));
            }
        }
        return materialQty;
    }

    public void checkWarehouseStockOnhand(Records record, Records detail, Records warehouse, String stockRule, String materialId, Records material) {
        if (warehouse.any()) {
            // sn 库存标签状态已经改为已分配了,   批次和数量的要校验
            double qty = detail.getDouble("qty");
            Records location = detail.getRec("location_id");
            Records stockOnhand = record.getEnv().get("stock.onhand");
            // 确定使用当前标签的时候,已经控制和修改过分配数
            if ("sn".equals(stockRule)) {
                // 序列号直接删除
                String labelId = detail.getRec("label_id").getId();
                stockOnhand.find(Criteria.equal("label_id", labelId)).delete();
            } else {
                Criteria criteria = null;
                if ("lot".equals(stockRule)) {
                    String lotNum = detail.getString("lot_num");
                    criteria = Criteria.equal("lot_num", lotNum)
                        .and(Criteria.equal("material_id",materialId))
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId()));
                } else {
                    criteria = Criteria.equal("material_id", materialId)
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", detail.getRec("location_id").getId()))
                        .and(Criteria.equal("lot_num", null))
                        .and(Criteria.equal("label_id", null));
                }
                // 扣减库存数量
                stockOnhand = stockOnhand.find(criteria);
                // 确定的时候,扣减了可用量,添加了分配数, 这里不需要校验
                //stockOnhand.set("usable_qty", Utils.round(stockOnhand.getDouble("usable_qty") - qty));
                stockOnhand.set("ok_qty", Utils.round(stockOnhand.getDouble("ok_qty") - qty));
                stockOnhand.set("allot_qty", Utils.round(stockOnhand.getDouble("allot_qty") - qty));
                if (Utils.equals(stockOnhand.getDouble("usable_qty"), 0d) && Utils.equals(stockOnhand.getDouble("ok_qty"), 0d) && Utils.equals(stockOnhand.getDouble("allot_qty"), 0d)) {
                    stockOnhand.delete();
                }
            }
        }
    }

    public void updateReturnSupplierStatus(Records records) {
        //执行sql前flush保存
        records.flush();
        Cursor cr = records.getEnv().getCursor();
        String sql = "select distinct status from wms_return_supplier_line where return_id=%s";
        for (Records record : records) {
            cr.execute(sql, Arrays.asList(record.getId()));
            List<String> status = cr.fetchAll().stream().map(r -> (String) r[0]).collect(Collectors.toList());
            boolean done = status.stream().allMatch(s -> "done".equals(s));
            if (done) {
                // 如果全部为完成状态，则更新为已完成状态
                record.set("status", "done");
                break;
            }
        }
    }

    public void writeBackPoLineReturnQty(Records record) {
        Records returnSupplierLine = record.getRec("line_ids");
        for (Records supplierLine : returnSupplierLine) {
            // 回写采购订单退货数,并且控制退货数不能超
            Records poId = supplierLine.getRec("po_id");
            Records material = supplierLine.getRec("material_id");
            // 既然是审核,需退货数肯定要全退
            // 需退货数
            double requestQty = supplierLine.getDouble("request_qty");
            // 获取采购订单明细数据
            Records purchaseOrderLine = record.getEnv().get("purchase.order_line")
                .find(Criteria.equal("material_id", material.getId()).and(Criteria.equal("po_id", poId.getId())));
            checkReturnQty(record, purchaseOrderLine, requestQty);
            // 出库的时候,会回写到采购订单
            //computeReturnQty(purchaseOrderLine, requestQty);
        }
    }

    public void computeReturnQty(Records purchaseOrderLine, double requestQty) {
        for (Records poLine : purchaseOrderLine) {
            // 当前采购订单明细的收货数量数量
            double receiveQty = poLine.getDouble("receive_qty");
            // 当前采购订单明细的退货数
            double returnQty = poLine.getDouble("return_qty");
            // 入库数量
            double stockInQty = poLine.getDouble("stock_in_qty");
            // todo 这里好像有点漏洞,存在空子的数据怎么处理
            //  比如: 送货2000 挑选退货1500, 那么有500 还没有真正入库,实际入库之前还有500 数量可操作,现在手动建单,如何控制
            double baseQty = Utils.round(receiveQty - returnQty - stockInQty);
            // 需退货数,大于 采购订单可退货数 ,
            if (Utils.lessOrEqual(requestQty, baseQty)) {
                // 需退货数 <= 可退数, 退完了,
                poLine.set("return_qty", Utils.round(poLine.getDouble("return_qty") + requestQty));
                break;
            }
            // 需退货数 > 可退数,
            // 前面已经判断过,需退货数,肯定是小于所有可退数量的,
            // 那么进来这里的数据, 说明明细有多条,
            poLine.set("return_qty", Utils.round(poLine.getDouble("return_qty") + baseQty));
            requestQty -= baseQty;
        }
    }

    public void checkReturnQty(Records record, Records purchaseOrderLine, double requestQty) {
        // 可能存在多条想通物料的数据吗,当他存在, 基本就一条
        double totalCanReturnQty = purchaseOrderLine.stream().mapToDouble(e ->
            Utils.round(e.getDouble("receive_qty") -
                e.getDouble("return_qty") -
                e.getDouble("stock_in_qty"))
        ).sum();
        if (Utils.lessOrEqual(totalCanReturnQty, 0d)) {
            throw new ValidationException(record.l10n("无可退数量,请检查数据"));
        }
        if (Utils.large(requestQty, totalCanReturnQty)) {
            throw new ValidationException(record.l10n("退货数大于可退货数,请检查数据"));
        }
    }
}
