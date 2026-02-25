package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;

@Model.Meta(name = "wms.other_stock_in", label = "其它入库", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class OtherStockIn extends Model {
    static Field code = Field.Char().label("单号").unique().readonly();
    static Field type = Field.Selection(new Options() {{
        put("customer", "客供料入库");
        put("supplier", "供应商赠送");
        put("plant", "车间入库");
        put("work_order", "小工单入库");
        put("maintenance", "维修品入库");
    }}).label("类型").required(true).defaultValue("customer");
    static Field supplier_id = Field.Many2one("md.supplier").label("供应商");
    static Field customer_id = Field.Many2one("md.customer").label("客户");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("收货仓库").required().lookup("searchWarehouse");
    static Field remark = Field.Char().label("备注");
    static Field line_ids = Field.One2many("wms.other_stock_in_line", "other_stock_in_id").label("物料明细");
    static Field details_ids = Field.One2many("wms.other_stock_in_details", "other_stock_in_id").label("扫码明细");

    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    @Model.ServiceMethod(label = "扫描确认", doc = "扫码以后确认功能")
    public Object receive(Records record, @Doc("物料") String materialId, @Doc("标签") String code, @Doc("数量") Double qty, @Doc("仓库") String warehouseId, @Doc("库位") String locationId) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        // 要么是自动确认,过来,或者是扫完确认, 不再校验标签
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records otherStockInLine = checkLine(record, materialId, qty);
        Records material = env.get("md.material", materialId);
        String stockRule = material.getString("stock_rule");
        selectStockRuleAndCreateDetails(record, materialId, warehouseId, locationId, qty, stockRule, codes);
        writeBackLineStatus(qty, otherStockInLine);
        Records lines = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and("material_id", "=", materialId));
        Map<String, Object> returnMaterial = getReturnMaterial(record, lines, warehouseId, locationId, code);
        result.put("message", record.l10n("条码[%s]使用成功", code));
        result.put("data", returnMaterial);
        return result;
    }

    public void writeBackLineStatus(Double qty, Records otherStockInLine) {
        for (Records line : otherStockInLine) {
            double requestQty = line.getDouble("request_qty");
            double scanQty = line.getDouble("scan_qty");
            if (Utils.large(requestQty, scanQty) && !Utils.equals(qty, 0d)) {
                // 还有没有确定的数量
                double roundQty = Utils.round(requestQty - scanQty);
                if (Utils.largeOrEqual(qty, roundQty)) {
                    line.set("scan_qty", line.getDouble("request_qty"));
                    qty = Utils.round(qty - roundQty);
                    line.set("status", "stocked");
                } else {
                    line.set("scan_qty", Utils.round(line.getDouble("scan_qty") + qty));
                    qty = 0d;
                    line.set("status", "stocking");
                }
            }
        }
    }

    public void selectStockRuleAndCreateDetails(Records record, String materialId, String warehouseId, String locationId, Double qty, String stockRule, String[] codes) {
        Environment env = record.getEnv();
        if ("sn".equals(stockRule)) {
            // 序列号
            String snCode = codes[0];
            // 创建明细数据
            Records materialLabel = env.get("lbl.material_label").find(Criteria.equal("sn", snCode));
            materialLabel.set("status", "allot");
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.other_stock_in");
            log.put("related_id", record.getId());
            log.put("related_code", record.getString("code"));
            materialLabel.call("logStatus", log);
            createOtherStockInDetails(record, qty, materialId, materialLabel.getId(), warehouseId, locationId, materialLabel.getString("lot_num"));
        } else if ("lot".equals(stockRule)) {
            String lotNumCode = codes[2];
            // 合并
            Records detail = record.getEnv().get("wms.other_stock_in_details").find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.equal("lot_num", lotNumCode)).and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId)));
            // 只可能有一个,多个就报错把
            String detailId = null;
            if (detail.any()) {
                detail.ensureOne();
                // 当前批次号已经存在了数据,并且库位相同,那就加数量
                detail.set("qty", Utils.round(detail.getDouble("qty") + qty));
                detail.set("stock_qty", Utils.round(detail.getDouble("stock_qty") + qty));
                detailId = detail.getId();
            } else {
                Records otherStockInDetails = createOtherStockInDetails(record, qty, materialId, null, warehouseId, locationId, lotNumCode);
                detailId = otherStockInDetails.getId();
            }
            boolean lotInQtyFlag = env.getConfig().getBoolean("lot_in_qty");
            if (lotInQtyFlag) {
                // 不存在的就新增到临时表
                Map<String, Object> data = new HashMap<>();
                data.put("order_id", record.getId());
                data.put("sn", codes[0]);
                data.put("material_id",materialId);
                data.put("lot_num", lotNumCode);
                data.put("detail_id", detailId);
                data.put("type", "wms.other_stock_in");
                env.get("lbl.lot_status").create(data);
            }
        } else {
            // 合并
            Records detail = record.getEnv().get("wms.other_stock_in_details").find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.equal("material_id", materialId)).and(Criteria.equal("warehouse_id", warehouseId)).and(Criteria.equal("location_id", locationId)));
            if (detail.any()) {
                detail.ensureOne();
                detail.set("qty", Utils.round(detail.getDouble("qty") + qty));
                detail.set("stock_qty", Utils.round(detail.getDouble("stock_qty") + qty));
            } else {
                createOtherStockInDetails(record, qty, materialId, null, warehouseId, locationId, null);
            }
        }
    }

    public Records createOtherStockInDetails(Records record, Double qty, String materialId, String labelId, String warehouseId, String locationId, String lotNumCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("other_stock_in_id", record.getId());
        data.put("qty", qty);
        data.put("label_id", labelId);
        data.put("material_id", materialId);
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        data.put("lot_num", lotNumCode);
        data.put("status", "to-stock");
        data.put("stock_qty", qty);
        return record.getEnv().get("wms.other_stock_in_details").create(data);
    }

    public Records checkLine(Records record, String materialId, Double qty) {
        Records otherStockInLine = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and("material_id", "=", materialId));
        double requestQty = 0d;
        double scanQty = 0d;
        for (Records line : otherStockInLine) {
            requestQty += line.getDouble("request_qty");
            scanQty += line.getDouble("scan_qty");
        }
        double deficitQty = Math.max(0, Utils.round(requestQty - scanQty));
        if (Utils.large(qty, deficitQty)) {
            throw new ValidationException("标签数量大于待入数量,请检查数据");
        }
        return otherStockInLine;
    }

    @ServiceMethod(label = "扫描标签", doc = "序列号直接操作入库,其他显示明细")
    public Object scanCode(Records record, String code, Boolean autoConfirm, String warehouseId, String locationId) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records materialLabel = env.get("lbl.material_label");
        // 需要支持成品标签
        if (codes.length > 1) {
            Records material = env.get("md.material").find(Criteria.equal("code", codes[1]));
            if (!material.any()) {
                throw new ValidationException(material.l10n("物料[%s]不存在", codes[1]));
            }
            Records lines = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and("material_id", "=", material.getId()));
            if (!lines.any()) {
                throw new ValidationException(lines.l10n("物料[%s]不在入库清单", material.get("code")));
            }
            Map<String, Object> returnMaterial = getReturnMaterial(record, lines, warehouseId, locationId, code);
            // 剩余需入库数
            double deficitQty = Utils.toDouble(returnMaterial.get("deficit_qty"));
            // 获取仓库,库位数据
            String stockRule = material.getString("stock_rule");
            // finished / semi-finished
            String category = material.getString("category");
            if ("sn".equals(stockRule) || "finished".equals(category) || "semi-finished".equals(category)) {
                return getSnLabelMap(record, code, codes[0], returnMaterial, deficitQty, autoConfirm, warehouseId, locationId);
            } else if ("lot".equals(stockRule)) {
                return getLotLabelMap(record, code, codes, returnMaterial, autoConfirm, warehouseId, locationId,material.getId());
            } else {
                return getNumLabelMap(record, code, Utils.toDouble(codes[codes.length - 1]), returnMaterial, autoConfirm, warehouseId, locationId);
            }
        } else if ((materialLabel = materialLabel.find(Criteria.equal("sn", code))).any()) {
            Records material = materialLabel.getRec("material_id");
            Records lines = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and("material_id", "=", material.getId()));
            if (!lines.any()) {
                throw new ValidationException(lines.l10n("物料[%s]不在入库清单", material.get("code")));
            }
            Map<String, Object> returnMaterial = getReturnMaterial(record, lines, warehouseId, locationId, code);
            double deficitQty = Utils.toDouble(returnMaterial.get("deficit_qty"));
            return getSnLabelMap(record, code, codes[0], returnMaterial, deficitQty, autoConfirm, warehouseId, locationId);
        }
        // 扫码的不是正常标签的格式, 那就直接当做物料解析,先不管其他类型
        // 直接扫物料
        return getMaterialMap(record, code, warehouseId, locationId);
    }

    public Map<String, Object> getNumLabelMap(Records record, String code, Double labelQty, Map<String, Object> returnMaterial,Boolean autoConfirm, String warehouseId, String locationId) {
        Map<String, Object> result = new HashMap<>();
        // 数量管控那就随便了, 只管当前标签的数量,需要修改数量就随意
        returnMaterial.put("qty", labelQty);
        result.put("data", returnMaterial);
        if (Utils.toBoolean(autoConfirm)) {
            result.putAll((Map<String, Object>) record.call("receive", returnMaterial.get("material_id"), code, labelQty, warehouseId, locationId));
            result.put("message", record.l10n("条码[%s]识别使用成功", code));
            return result;
        }
        result.put("message", record.l10n("条码[%s]扫码成功", code));
        return result;
    }

    public Map<String, Object> getLotLabelMap(Records record, String code, String[] codes, Map<String, Object> returnMaterial, Boolean autoConfirm, String warehouseId, String locationId,String materialId) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        String lotNumCode = codes[2];
        double labelQty = Utils.toDouble(codes[codes.length - 1]);
        boolean lotInQtyFlag = env.getConfig().getBoolean("lot_in_qty");
        if (lotInQtyFlag) {
            Records lotSnTransient = env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId())
                .and(Criteria.equal("type", "wms.other_stock_in")).and(Criteria.equal("material_id",materialId))
                .and(Criteria.equal("lot_num", lotNumCode)).and(Criteria.equal("sn", codes[0])));
            if (lotSnTransient.any()) {
                // 存在,
                throw new ValidationException(record.l10n("当前批次标签已使用,序列号[%s],请扫描其他标签", codes[0]));
            }
        }
        // 先看看这个批次号是否存在
        Records lotNum = env.get("lbl.lot_num").find(Criteria.equal("code", lotNumCode).and(Criteria.equal("material_id",materialId)));
        if (!lotNum.any()) {
            throw new ValidationException(record.l10n("当前批次标签无法识别,请检查数据"));
        }
        returnMaterial.put("qty", labelQty);
        returnMaterial.put("lot_in_qty", lotInQtyFlag);
        result.put("data", returnMaterial);
        if (Utils.toBoolean(autoConfirm)) {
            result.putAll((Map<String, Object>) record.call("receive", returnMaterial.get("material_id"), code, labelQty, warehouseId, locationId));
            result.put("message", record.l10n("条码[%s]识别使用成功", code));
            return result;
        }
        result.put("message", record.l10n("条码[%s]扫码成功", code));
        return result;
    }

    public Map<String, Object> getMaterialMap(Records record, String code, String warehouseId, String locationId) {
        Map<String, Object> result = new HashMap<>();
        Records material = record.getEnv().get("md.material").find(Criteria.equal("code", code));
        Records lines = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and("material_id", "=", material.getId()));
        if (!lines.any()) {
            throw new ValidationException(lines.l10n("物料[%s]不在入库清单", material.get("code")));
        }
        Map<String, Object> data = getReturnMaterial(record, lines, warehouseId, locationId, code);
        result.put("action", "material");
        result.put("data", data);
        result.put("message", lines.l10n("条码[%s]读取物料成功", code));
        return result;
    }

    public Map<String, Object> getReturnMaterial(Records record, Records lines, String warehouseId, String locationId, String code) {
        double requestQty = 0d;
        double scanQty = 0d;
        for (Records line : lines) {
            requestQty += line.getDouble("request_qty");
            scanQty += line.getDouble("scan_qty");
        }
        double deficitQty = Math.max(0, Utils.round(requestQty - scanQty));
        Records material = lines.first().getRec("material_id");
        Records unit = material.getRec("unit_id");
        Map<String, Object> data = new HashMap<>();
        data.put("material_id", material.getId());
        data.put("request_qty", requestQty);
        data.put("scan_qty", scanQty);
        data.put("deficit_qty", deficitQty);
        data.put("material_name_spec", material.get("name_spec"));
        data.put("material_category", material.get("category"));
        data.put("stock_rule", material.get("stock_rule"));
        data.put("unit_id", unit.getPresent());
        data.put("warehouse_id", warehouseId);
        data.put("location_id", locationId);
        data.put("sn", code);
        data.put("unit_accuracy", unit.getInteger("accuracy"));
        return data;
    }

    public Map<String, Object> getSnLabelMap(Records record, String code, String sn, Map<String, Object> returnMaterial, double deficitQty, Boolean autoConfirm, String warehouseId, String locationId) {
        Environment env = record.getEnv();
        Map<String, Object> result = new HashMap<>();
        Records label = env.get("lbl.material_label").find(Criteria.equal("sn", sn));
        if (!label.any()) {
            throw new ValidationException(record.l10n("当前标签不存在,请检查数据"));
        }
        Records details = env.get("wms.other_stock_in_details").find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.equal("label_id", label.getId())));
        if (details.any()) {
            throw new ValidationException(record.l10n("标签[%s]已使用,请检查数据", code));
        }
        double labelQty = label.getDouble("qty");
        Records onhand = env.get("stock.onhand").find(Criteria.equal("label_id", label.getId()));
        if (onhand.any()) {
            throw new ValidationException(record.l10n("标签[%s]已在库,请检查数据", code));
        } else {
            returnMaterial.put("qty", labelQty);
        }
        // 这种事情,就不拆分了,本来就是乱入,标签还打多,直接给报错
        if (Utils.less(deficitQty, labelQty)) {
            throw new ValidationException(record.l10n("标签[%s]扫码数量大于剩余待入库数,请重新生成标签", code));
        }
        result.put("data", returnMaterial);
        if (Utils.toBoolean(autoConfirm)) {
            result.putAll((Map<String, Object>) record.call("receive", returnMaterial.get("material_id"), code, labelQty, warehouseId, locationId));
            result.put("message", record.l10n("条码[%s]识别使用成功", code));
            return result;
        }
        returnMaterial.put("qty", labelQty);
        result.put("message", record.l10n("条码[%s]扫码成功", code));
        return result;
    }

    public void checkLabel(Records record, String code, Records material, Records materialLabel, Double qty, Records line) {
        // 查看当前标签是否已经被使用
        Records otherStockInDetails = record.getEnv().get("wms.other_stock_in_details");
        if (materialLabel.any()) {
            otherStockInDetails = otherStockInDetails.find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.equal("label_id", materialLabel.getId())));
        }
        if (otherStockInDetails.any()) {
            throw new ValidationException(record.l10n("条码[%s]已有记录，不能重复使用", code));
        }
        // 查一下标签是否在库
        Records stockOnhand = record.getEnv().get("stock.onhand").find(Criteria.equal("sn", code));
        if (stockOnhand.any()) {
            throw new ValidationException(record.l10n("标签[%s]在库,请检查标签数据", code));
        }
    }

    public Map<String, Object> getResultMap(Records record, String code, String warehouseId, Double qty, Records material, Records line) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("qty", qty);
        resultMap.put("sn", code);
        resultMap.put("material_id", material.getId());
        resultMap.put("material_name_spec", material.get("name_spec"));
        resultMap.put("material_code", material.getString("code"));
        resultMap.put("warehouse_id", warehouseId);
        resultMap.put("request_qty", line.getDouble("request_qty"));
        resultMap.put("scan_qty", line.getDouble("scan_qty"));
        resultMap.put("id", record.getId());
        resultMap.put("status", record.getString("status"));
        return resultMap;
    }

    @ServiceMethod(label = "入库", doc = "根据入库明细生成入库单")
    public Object stockIn(Records records) {
        Environment env = records.getEnv();
        Records stockOnhand = env.get("stock.onhand");
        for (Records record : records) {
            Records otherStockInDetails = env.get("wms.other_stock_in_details")
                .find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.equal("status", "to-stock")).and(Criteria.equal("stock_in_id", null)));
            if (!otherStockInDetails.any()) {
                throw new ValidationException("当前无可提交明细数据,请检查入库明细");
            }
            // 生成入库单
            Map<String, Object> stockInData = new HashMap<>();
            stockInData.put("type", "wms.other_stock_in");
            stockInData.put("related_code", record.getString("code"));
            stockInData.put("related_model", "wms.other_stock_in");
            stockInData.put("related_id", record.getId());
            stockInData.put("status", "done");
            Records stockIn = env.get("stock.stock_in").create(stockInData);
            otherStockInDetails.set("stock_in_id", stockIn.getId());
            otherStockInDetails.set("status", "done");
            // 直接生成 库存数据
            List<Map<String, Object>> onhandList = new ArrayList<>();
            for (Records detail : otherStockInDetails) {
                Records warehouse = detail.getRec("warehouse_id");
                Records location = detail.getRec("location_id");
                Records label = detail.getRec("label_id");
                Records material = detail.getRec("material_id");
                String stockRule = material.getString("stock_rule");
                Double qty = detail.getDouble("qty");
                Map<String, Object> onhandData = new HashMap<>();
                if ("sn".equals(stockRule)) {
                    // 序列号 直接生成库存数据
                    onhandData.put("material_id", material.getId());
                    onhandData.put("ok_qty", qty);
                    onhandData.put("usable_qty", qty);
                    onhandData.put("sn", detail.getString("sn"));
                    onhandData.put("company_id", warehouse.getRec("company_id").getId());
                    onhandData.put("label_id", label.getId());
                    onhandData.put("warehouse_id", warehouse.getId());
                    onhandData.put("location_id", location.getId());
                    onhandData.put("stock_in_time", new Date());
                    onhandData.put("status", "onhand");
                    onhandData.put("lot_num", detail.getString("lot_num"));
                    onhandList.add(onhandData);
                    label.set("status", "onhand");
                    Map<String, Object> log = new HashMap<>();
                    log.put("operation", "wms.other_stock_in:stock_in");
                    log.put("related_id", record.getId());
                    log.put("related_code", record.getString("code"));
                    label.call("logStatus", log);
                } else if ("lot".equals(stockRule)) {
                    // 批次管控  查询是否存在相同的库存数据
                    Records onhand = stockOnhand.find(Criteria.equal("material_id", material.getId())
                        .and(Criteria.equal("lot_num", detail.getString("lot_num")))
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId())));
                    if (onhand.any()) {
                        // 有库存
                        onhand.set("ok_qty", onhand.getDouble("ok_qty") + qty);
                        onhand.set("usable_qty", onhand.getDouble("usable_qty") + qty);
                    } else {
                        // 无库存
                        onhandData.put("material_id", material.getId());
                        onhandData.put("ok_qty", qty);
                        onhandData.put("usable_qty", qty);
                        onhandData.put("company_id", warehouse.getRec("company_id").getId());
                        onhandData.put("warehouse_id", warehouse.getId());
                        onhandData.put("location_id", location.getId());
                        onhandData.put("stock_in_time", new Date());
                        onhandData.put("status", "onhand");
                        onhandData.put("lot_num", detail.getString("lot_num"));
                        onhandList.add(onhandData);
                    }
                } else {
                    // 数量管控 查询是否存在相同的库存数据
                    Records onhand = stockOnhand.find(Criteria.equal("material_id", material.getId())
                        .and(Criteria.equal("warehouse_id", warehouse.getId()))
                        .and(Criteria.equal("location_id", location.getId())));
                    if (onhand.any()) {
                        // 有库存
                        onhand.set("ok_qty", onhand.getDouble("ok_qty") + qty);
                        onhand.set("usable_qty", onhand.getDouble("usable_qty") + qty);
                    } else {
                        // 无库存
                        onhandData.put("material_id", material.getId());
                        onhandData.put("ok_qty", qty);
                        onhandData.put("usable_qty", qty);
                        onhandData.put("company_id", warehouse.getRec("company_id").getId());
                        onhandData.put("warehouse_id", warehouse.getId());
                        onhandData.put("location_id", location.getId());
                        onhandData.put("stock_in_time", new Date());
                        onhandData.put("status", "onhand");
                        onhandList.add(onhandData);
                    }
                }
            }
            changeStatus(record);
            stockOnhand.createBatch(onhandList);
            env.get("lbl.lot_status").find(Criteria.equal("order_id", record.getId()).and(Criteria.equal("type", "wms.other_stock_in"))).delete();
        }
        String body = records.l10n("生成入库单");
        records.call("trackMessage", body);
        return Action.reload(records.l10n("操作成功"));
    }

    // 变更数据状态
    // 没扫满 返回生成入库单回写erp，很麻烦
    public void changeStatus(Records record) {
        Records lineIds = record.getEnv().get("wms.other_stock_in_line").find(Criteria.equal("other_stock_in_id", record.getId()).and(Criteria.notEqual("status", "done")));
        boolean flag = true;
        for (Records line : lineIds) {
            if ("stocked".equals(line.getString("status"))) {
                line.set("status", "done");
            } else {
                flag = false;
            }
        }
        if (flag) {
            record.set("status", "done");
        }
    }
}
