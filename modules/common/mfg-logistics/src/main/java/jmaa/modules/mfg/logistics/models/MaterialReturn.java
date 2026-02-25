package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;
import org.jmaa.sdk.util.KvMap;

import java.util.*;

@Model.Meta(name = "mfg.material_return", label = "生产退料", inherit = {"code.auto_code", "mixin.order_status"})
public class MaterialReturn extends Model {
    static Field code = Field.Char().label("退料单号").readonly().unique();
    static Field workshop_id = Field.Many2one("md.workshop").label("工厂/车间");
    static Field related_code = Field.Char().label("相关单据");
    static Field related_id = Field.Many2oneReference("related_model").label("相关单据");
    static Field related_model = Field.Char().label("相关模型");
    static Field remark = Field.Char().label("备注");
    static Field in_charge_dept = Field.Many2one("md.enterprise_model").label("责任部门");
    static Field return_reason = Field.Char().label("退料原因");
    static Field return_type = Field.Selection(new LinkedHashMap<String, String>() {{
        put("workorder", "工单退料");
        put("defect", "不良退料");
        put("transfer", "转产退料");
    }}).label("退料类型").required().defaultValue("workorder");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("退料仓库");
    static Field line_ids = Field.One2many("mfg.material_return_line", "material_return_id").label("物料列表");
    static Field details_ids = Field.One2many("mfg.material_return_details", "material_return_id").label("物料明细");
    static Field status = Field.Selection(new Options() {{
        put("draft", "草稿");
        put("returning", "退料中");
        put("done", "完成");
    }}).label("状态").required().defaultValue("draft").tracking();

    @ServiceMethod(label = "扫描标签", doc = "序列号直接退,其他显示明细", auth = "commit")
    public Object scanCode(Records record, String code) {
        Environment env = record.getEnv();
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material");
        if (codes.length > 1) {
            material = material.find(Criteria.equal("code", codes[1]));
            String stockRule = material.getString("stock_rule");
            if ("sn".equals(stockRule)) {
                Records label = env.get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
                if (!label.any()) {
                    throw new ValidationException("标签不存在,请使用正确标签");
                }
                Records details = env.get("mfg.material_return_details").find(Criteria.equal("material_return_id", record.getId()).and(Criteria.equal("label_id", label.getId())));
                if (details.any()) {
                    throw new ValidationException(record.l10n("当前标签[%s]已扫描,如需修改,请删除后,再次扫描", codes[0]));
                }
                return returnByLabel(record, label, code);
            } else if ("lot".equals(stockRule)) {
                String lotNum = codes[2];
                return returnByLotAndNum(record, lotNum, material, code, codes[codes.length - 1]);
            } else {
                // 数量
                return returnByLotAndNum(record, null, material, code, codes[codes.length - 1]);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("data", Collections.emptyMap());
        result.put("message", record.l10n("条码[%s]无法识别,请输入完整条码数据", code));
        return result;
    }

    public Object returnByLabel(Records record, Records label, String code) {
        Map<String, Object> result = new HashMap<>();
        Records material = label.getRec("material_id");
        Map<String, Object> returnMaterial = getReturnMaterial(record, material, label.getDouble("qty"));
        returnMaterial.put("sn", code);
        result.put("data", returnMaterial);
        result.put("message", record.l10n("条码[%s]扫描识别成功", label.get("sn")));
        return result;
    }

    public Map<String, Object> returnByLotAndNum(Records record, String lotNum, Records material, String code, String qty) {
        Map<String, Object> dataMap = getReturnMaterial(record, material, Utils.toDouble(qty));
        dataMap.put("sn", code);
        Map<String, Object> result = new HashMap<>();
        result.put("message", record.l10n("条码[%s]扫码识别成功", code));
        result.put("data", dataMap);
        return result;
    }

    public Map<String, Object> getReturnMaterial(Records record, Records material, double returnedQty) {
        Map<String, Object> result = new HashMap<>();
        String stockRule = material.getString("stock_rule");
        Records returnWarehouse = record.getRec("warehouse_id");
        Records warehouses = (Records) material.call("findWarehouses");
        // 物料配置过, 标签添加过,
        if (warehouses.any() && returnWarehouse.any() && !warehouses.contains(returnWarehouse)) {
            // 不在配置中
            result.put("warehouse_id", null);
        } else if (warehouses.any() && returnWarehouse.any()) {
            result.put("warehouse_id", returnWarehouse.getId());
        } else {
            result.put("warehouse_id", warehouses.first().getId());
        }
        if ("sn".equals(stockRule)) {
            result.put("id", record.getId());
            result.put("status", record.get("status"));
            result.put("material_id", material.getPresent());
            result.put("material_code", material.getString("code"));
            result.put("material_name_spec", material.get("name_spec"));
            result.put("return_qty", returnedQty);
            result.put("stock_rule", material.get("stock_rule"));
            result.put("abc_type", material.getSelection("abc_type"));
            result.put("unit", material.getRec("unit_id").get("present"));
            result.put("accuracy", material.getRec("unit_id").get("accuracy"));
        } else {
            result.put("id", record.getId());
            result.put("status", record.get("status"));
            result.put("material_id", material.getPresent());
            result.put("material_code", material.getString("code"));
            result.put("material_name_spec", material.get("name_spec"));
            result.put("return_qty", returnedQty);
            result.put("stock_rule", material.get("stock_rule"));
            result.put("abc_type", material.getSelection("abc_type"));
            result.put("unit", material.getRec("unit_id").get("present"));
            result.put("accuracy", material.getRec("unit_id").get("accuracy"));
        }
        return result;
    }

    @ServiceMethod(label = "扫码退料", doc = "扫码后确认按钮", auth = "commit")
    public Object returnMaterial(Records record, @Doc("标签") String code, @Doc("退料数量") Double returnedQty,
                                 @Doc("是否打印原标签") Boolean printFlag,
                                 @Doc("标签模板") String templateId, @Doc("物料") String materialId, @Doc("仓库") String warehouseId) {
        Environment env = record.getEnv();
        if ("draft".equals(record.getString("status"))) {
            record.set("status", "returning");
        }
        String[] codes = (String[]) env.get("lbl.code_parse").call("parse", code);
        Records material = env.get("md.material", materialId);
        String stockRule = material.getString("stock_rule");
        Map<String, Object> resultMap = null;
        // 前一步都校验过了,这里直接用
        if ("sn".equals(stockRule)) {
            resultMap = materialReturnBySn(record, code, returnedQty, printFlag, templateId, codes, material, warehouseId);
        } else if ("lot".equals(stockRule)) {
            resultMap = materialReturnByLot(record, code, returnedQty, printFlag, templateId, codes, material, warehouseId);
        } else {
            resultMap = materialReturnByNum(record, code, returnedQty, printFlag, templateId, codes, material, warehouseId);
        }
        // 弹框数据
        resultMap.put("dialogData", getReturnMaterial(record, material, returnedQty));
        return resultMap;
    }

    public Map<String, Object> materialReturnByNum(Records record, String code, Double returnedQty, Boolean printFlag, String templateId, String[] codes, Records material, String warehouseId) {
        Map<String, Object> resultMap = new HashMap<>();
        Records materialReturnDetails = record.getEnv().get("mfg.material_return_details");
        Records detail = materialReturnDetails.find(Criteria.equal("material_return_id", record.getId()).and(Criteria.equal("material_id", material.getId())));
        if (detail.any()) {
            // 存在
            detail.set("qty", Utils.round(detail.getDouble("qty") + returnedQty));
            resultMap.put("message", record.l10n("条码[%s]合并成功", code));
        } else {
            Map<String, Object> createMap = new HashMap<>();
            createMap.put("material_id", material.getId());
            createMap.put("qty", returnedQty);
            createMap.put("warehouse_id", warehouseId);
            createMap.put("status", "to-stock");
            createMap.put("material_return_id", record.getId());
            materialReturnDetails.create(createMap);
            resultMap.put("message", record.l10n("条码[%s]使用成功", code));
        }
        if (printFlag) {
            Map<String, Object> labelData = new HashMap<>();
            labelData.put("code", ((List<String>)record.getEnv().get("lbl.material_label").call("createCodes", material.getId(), 1)).get(0));
            labelData.put("material_code", material.getString("code"));
            labelData.put("material_name", material.getString("name"));
            labelData.put("material_spec", material.getString("spec"));
            labelData.put("print_time", new Date());
            labelData.put("qty", returnedQty);
            labelData.put("sn", codes[0]);
            labelData.put("unit", material.getRec("unit_id").getString("name"));
            Records printTemplate = record.getEnv().get("print.template", templateId);
            resultMap.put("data", printTemplate.call("print", new KvMap().set("data", Utils.asList(labelData))));
        }
        return resultMap;
    }

    public Map<String, Object> materialReturnByLot(Records record, String code, Double returnedQty, Boolean printFlag, String templateId, String[] codes, Records material, String warehouseId) {
        // 相同批次,合并, 如果不合并, 后面生成入库单的时候,明细还是需要合并,并且还难处理
        Records materialReturnDetails = record.getEnv().get("mfg.material_return_details");
        Records lotNum = record.getEnv().get("lbl.lot_num").find(Criteria.equal("code", codes[2]));
        if (!lotNum.any()) {
            // 这里一般情况进不来,除非扫完,回车,不确认,直接复制一个新标签,不回车,直接确认,就可能进来
            throw new ValidationException(record.l10n("当前批次标签不存在,请检查标签数据"));
        }
        Map<String, Object> resultMap = new HashMap<>();
        Records detail = materialReturnDetails.find(Criteria.equal("material_return_id", record.getId()).and(Criteria.equal("lot_num", codes[2])));
        if (detail.any()) {
            // 存在
            detail.set("qty", Utils.round(detail.getDouble("qty") + returnedQty));
            resultMap.put("message", record.l10n("条码[%s]合并成功", code));
        } else {
            Map<String, Object> createMap = new HashMap<>();
            createMap.put("material_id", material.getId());
            createMap.put("qty", returnedQty);
            createMap.put("warehouse_id", warehouseId);
            createMap.put("status", "to-stock");
            createMap.put("material_return_id", record.getId());
            createMap.put("lot_num", codes[2]);
            materialReturnDetails.create(createMap);
            resultMap.put("message", record.l10n("条码[%s]使用成功", code));
        }
        if (printFlag) {
            String lotnumQty = codes[codes.length - 1];
            String substring = code.substring(0, code.length() - lotnumQty.length());
            Records supplier = lotNum.getRec("supplier_id");
            Map<String, Object> labelData = new KvMap()
                .set("code", substring + returnedQty)
                .set("sn", codes[0])
                .set("lot_num", codes[2])
                .set("qty", returnedQty)
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("unit", material.getRec("unit_id").get("name"))
                .set("product_date", Utils.format(lotNum.getDate("product_date"), "yyyy-MM-dd"))
                .set("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"))
                .set("supplier_chars", supplier.get("chars"))
                .set("supplier_code", supplier.get("code"))
                .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            Records printTemplate = record.getEnv().get("print.template", templateId);
            resultMap.put("data", printTemplate.call("print", new KvMap().set("data", Utils.asList(labelData))));
        }
        return resultMap;
    }

    public Map<String, Object> materialReturnBySn(Records record, String code, Double returnedQty, Boolean printFlag, String templateId, String[] codes, Records material, String warehouseId) {
        Records materialReturnDetails = record.getEnv().get("mfg.material_return_details");
        Records label = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", codes[0]));
        if (!label.any()) {
            throw new ValidationException(record.l10n("当前序列号标签不存在,请检查数据"));
        }
        Map<String, Object> log = new HashMap<>();
        log.put("operation", "mfg.material_return");
        log.put("related_id", record.getId());
        log.put("related_code", record.getString("code"));
        label.call("logStatus", log);
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> createMap = new HashMap<>();
        createMap.put("material_id", material.getId());
        createMap.put("qty", returnedQty);
        createMap.put("warehouse_id", warehouseId);
        createMap.put("status", "to-stock");
        createMap.put("material_return_id", record.getId());
        createMap.put("label_id", label.getId());
        materialReturnDetails.create(createMap);
        resultMap.put("message", record.l10n("条码[%s]使用成功", code));
        if (printFlag) {
            Records printTemplate = record.getEnv().get("print.template", templateId);
            Records supplier = label.getRec("supplier_id");
            Map<String, Object> labelData = new KvMap()
                .set("code", code)
                .set("sn", codes[0])
                .set("qty", returnedQty)
                .set("material_code", material.get("code"))
                .set("material_name", material.get("name"))
                .set("material_spec", material.get("spec"))
                .set("unit", material.getRec("unit_id").get("name"))
                .set("product_date", Utils.format(label.getDate("product_date"), "yyyy-MM-dd"))
                .set("supplier_name", Utils.replaceAll(supplier.getString("name"), ",", "，"))
                .set("supplier_chars", supplier.get("chars"))
                .set("supplier_code", supplier.get("code"))
                .set("print_time", Utils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
            resultMap.put("data", printTemplate.call("print", new KvMap().set("data", Utils.asList(labelData))));
        }
        return resultMap;
    }

    @Model.ServiceMethod(label = "生成入库单", doc = "生成入库单")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("done".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
            Records details = record.getEnv().get("mfg.material_return_details").find(Criteria.equal("material_return_id", record.getId()));
            //Records details = record.getRec("details_ids");
            if (!details.any()) {
                throw new ValidationException(record.l10n("无退料明细数据"));
            }
            // 合并物料数量
            Map<String, Object> returnLineMap = new HashMap<>();
            for (Records detail : details) {
                Records label = detail.getRec("label_id");
                if (label.any()) {
                    label.set("qty", detail.getDouble("qty"));
                    label.set("status", "to-stock");
                }
                Records material = detail.getRec("material_id");
                double qty = detail.getDouble("qty");
                if (returnLineMap.containsKey(material.getId())) {
                    returnLineMap.put(material.getId(), Utils.round(Utils.toDouble(returnLineMap.get(material.getId())) + qty));
                    continue;
                }
                returnLineMap.put(material.getId(), qty);
            }
            // 生成入库单
            Records stockIn = record.getEnv().get("stock.stock_in");
            if (stockIn.getMeta().isAuto()) {
                Map<String, Object> stockInData = new HashMap<>();
                stockInData.put("type", "mfg.material_return");
                stockInData.put("related_code", record.getString("code"));
                stockInData.put("related_model", "mfg.material_return");
                stockInData.put("related_id", record.getId());
                stockInData.put("status", "new");
                stockIn = stockIn.create(stockInData);
                details.set("stock_in_id", stockIn.getId());
            }
            if (!returnLineMap.isEmpty()) {
                List<Map<String, Object>> returnLineList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : returnLineMap.entrySet()) {
                    Map<String, Object> insertLineMap = new HashMap<>();
                    insertLineMap.put("material_return_id", record.getId());
                    insertLineMap.put("status", "done");
                    insertLineMap.put("return_qty", entry.getValue());
                    insertLineMap.put("material_id", entry.getKey());
                    returnLineList.add(insertLineMap);
                }
                record.getEnv().get("mfg.material_return_line").createBatch(returnLineList);
            }
            record.getRec("line_ids").set("status", "done");
        }
        if (values != null) {
            records.update(values);
        }
        String body = records.l10n("提交") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "done");
        return Action.success();
    }
}
