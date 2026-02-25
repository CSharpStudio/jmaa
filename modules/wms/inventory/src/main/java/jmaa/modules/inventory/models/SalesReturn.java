package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "wms.sales_return", label = "销售退货", inherit = {"code.auto_code", "mixin.company", "mixin.order_status"})
public class SalesReturn extends Model {
    static Field code = Field.Char().label("退货单号").readonly().unique();
    static Field customer_id = Field.Many2one("md.customer").label("客户").required();
    static Field warehouse_id = Field.Many2one("md.warehouse").label("退货仓库").required().lookup("searchWarehouse");
    static Field line_ids = Field.One2many("wms.sales_return_line", "return_id").label("物料明细");
    static Field details_ids = Field.One2many("wms.sales_return_details", "return_id").label("退货明细");
    static Field remark = Field.Char().label("备注");

    public Criteria searchWarehouse(Records rec) {
        return Criteria.equal("user_ids", rec.getEnv().getUserId());
    }

    @Model.ServiceMethod(label = "收货", doc = "扫码以后确认功能")
    public Object receive(Records record, @Doc("仓库") String warehouseId, @Doc("标签") String code, @Doc("数量") Double qty) {
        if (Utils.isBlank(warehouseId)) {
            throw new ValidationException(record.l10n("请选择入库仓库"));
        }
        Environment env = record.getEnv();
        Records batchCode = null;
        if (env.getRegistry().contains("wip.batch_code")) {
            batchCode = env.get("wip.batch_code");
        }
        Records mdPackage = null;
        if (env.getRegistry().contains("packing.package")) {
            mdPackage = env.get("packing.package");
        }
        Records salesReturnDetails = record.getEnv().get("wms.sales_return_details");
        Records materialLable = record.getEnv().get("lbl.material_label").find(Criteria.equal("sn", code));
        Map<String, Object> resultData = new HashMap<>();
        Records material = null;
        Map<String, Object> createMap = new HashMap<>();
        createMap.put("qty", qty);
        createMap.put("warehouse_id", warehouseId);
        createMap.put("status", "new");
        createMap.put("return_id", record.getId());
        if (materialLable.any()) {
            if (salesReturnDetails.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("sn", code))).any()) {
                throw new ValidationException(record.l10n("当前标签已使用,请检查标签数据"));
            }
            // 标签数量可能变吗,可能
            if (!Utils.equals(materialLable.getDouble("qty"), qty)) {
                materialLable.set("qty", qty);
            }
            Map<String, Object> log = new HashMap<>();
            log.put("operation", "wms.sales_return");
            log.put("related_id", record.getId());
            log.put("related_code", record.getString("code"));
            materialLable.call("logStatus", log);
            material = materialLable.getRec("material_id");
            createMap.put("material_id", material.getId());
            createMap.put("lot_num", materialLable.getString("lot_num"));
            createMap.put("label_id", materialLable.getId());
            resultData.put("message", record.l10n("条码[%s]使用成功", code));
        } else if (null != batchCode && (batchCode = batchCode.find(Criteria.equal("code", code))).any()) {
            // 批次标签  待定 todo
            if (salesReturnDetails.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("lot_num", code))).any()) {
                throw new ValidationException(record.l10n("当前标签已使用,请检查标签数据"));
            }
            if (Utils.equals(batchCode.getDouble("qty"), qty)) {
                batchCode.set("qty", qty);
            }
            material = batchCode.getRec("material_id");
            createMap.put("material_id", material.getId());
            createMap.put("lot_num", code);
            resultData.put("message", record.l10n("批次条码[%s]使用成功", code));
        } else if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            if (mdPackage.getRec("parent_id").any()) {
                throw new ValidationException(record.l10n("包装标签请扫描最外箱标签码"));
            }
            if (Utils.equals(mdPackage.getDouble("package_qty"), qty)) {
                mdPackage.set("package_qty", qty);
            }
            material = mdPackage.getRec("material_id");
            createMap.put("material_id", material.getId());
            createMap.put("sn", code);
            resultData.put("message", record.l10n("条码[%s]使用成功", code));
        } else {
            // 这种基本不可能, 除非,扫码,确定之前,改了条码(误触)并且没有回车
            throw new ValidationException(record.l10n("标签[%s]无法识别,请检查标签数据", code));
        }
        salesReturnDetails.create(createMap);
        if ("draft".equals(record.getString("status"))) {
            record.set("status", "stocking");
        }
        Records line = env.get("wms.sales_return_line").find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("material_id", material.getId())));
        // 校验一下汇总的扫码数量,不能大于计划数量
        double requestQty = line.getDouble("request_qty");
        double returnedQty = line.getDouble("return_qty");
        returnedQty = Utils.round(returnedQty + qty);
        if (requestQty < returnedQty) {
            throw new ValidationException(record.l10n("累计扫码数量[%s](包含本次)大于计划数[%s],不能扫码", returnedQty, requestQty));
        }
        line.set("return_qty", returnedQty);
        resultData.put("data", getResultMap(record, code, warehouseId, qty, material, line));
        return resultData;
    }

    @ServiceMethod(label = "扫描标签", doc = "序列号直接退,其他显示明细")
    public Object scanCode(Records record, String code, Boolean autoConfirm, String warehouseId) {
        // 仓库只管成品标签,和包装标签,packing.package
        // 成品标签存 lbl.material_label 表,  二维码就一个序列码,没分隔符
        Map<String, Object> result = new HashMap<>();
        Environment env = record.getEnv();
        if (Utils.toBoolean(autoConfirm) && Utils.isBlank(warehouseId)) {
            throw new ValidationException(record.l10n("请选择入库仓库"));
        }
        Records materialLabel = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        Records batchCode = null;
        //  batchCode = env.get("wip.batch_code") ;  这种直接报错了,用不了 batchCode.getMeta().isAuto()
        if (env.getRegistry().contains("wip.batch_code")) {
            batchCode = env.get("wip.batch_code");
        }
        Records mdPackage = null;
        if (env.getRegistry().contains("packing.package")) {
            mdPackage = env.get("packing.package");
        }
        double qty = 0d;
        Records material = null;
        if (materialLabel.any()) {
            material = materialLabel.getRec("material_id");
            // 物料配置过, 标签添加过,
            qty = materialLabel.getDouble("qty");
        } else if (null != batchCode && (batchCode = batchCode.find(Criteria.equal("code", code))).any()) {
            // 批次标签 todo 暂时不处理这种标签
            material = batchCode.getRec("material_id");
            qty = batchCode.getDouble("qty");
        } else if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            qty = mdPackage.getDouble("package_qty");
            material = mdPackage.getRec("material_id");
        } else {
            throw new ValidationException(record.l10n("条码[%s]非成品标签,无法识别", code));
        }
        if (null == material || !material.any()) {
            throw new ValidationException(record.l10n("条码[%s]无法识别", code));
        }
        Records line = record.getEnv().get("wms.sales_return_line").find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("material_id", material.getId())));
        if (!line.any()) {
            throw new ValidationException(record.l10n("条码[%s]非当前单据所需物料", code));
        }
        checkLabel(record, code, material, materialLabel, batchCode, mdPackage, qty, line);
        if (Utils.toBoolean(autoConfirm)) {
            result.putAll((Map<String, Object>) record.call("receive", warehouseId, code, qty));
            result.put("message", record.l10n("条码[%s]识别使用成功", code));
            return result;
        }
        // 销售退货,实际使用的是material_label, 就算是批次管控也是有单独的标签表数据, 多次扫码已经控制,无需添加临时表数据,只控制只读
        Map<String, Object> resultMap = getResultMap(record, code, warehouseId, qty, material, line);
        resultMap.put("lot_in_qty", env.getConfig().getBoolean("lot_in_qty"));
        result.put("data", resultMap);
        result.put("message", record.l10n("条码[%s]识别成功", code));
        return result;
    }

    public void checkLabel(Records record, String code, Records material, Records materialLabel, Records batchCode, Records mdPackage, Double qty, Records line) {
        // 查看当前标签是否已经被使用
        Records salesReturnDetails = record.getEnv().get("wms.sales_return_details");
        if (materialLabel.any()) {
            salesReturnDetails = salesReturnDetails.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("label_id", materialLabel.getId())));
        }
        if (null != batchCode && batchCode.any()) {
            salesReturnDetails = salesReturnDetails.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("lot_num", code)));
        }
        if (null != mdPackage && mdPackage.any()) {
            salesReturnDetails = salesReturnDetails.find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("sn", mdPackage.getString("code"))));
        }
        if (salesReturnDetails.any()) {
            throw new ValidationException(record.l10n("条码[%s]已有记录，不能重复使用", code));
        }
        // 查一下标签是否在库
        Records stockOnhand = record.getEnv().get("stock.onhand").find(Criteria.equal("sn", code));
        if (stockOnhand.any()) {
            throw new ValidationException(record.l10n("标签[%s]在库,请检查标签数据", code));
        }
        // 销售只管成品,这里也是,后续可能需要更改 todo
        String category = material.getString("category");
        // "semi-finished", "半成品"  "finished", "成品"
        if (!"finished".equals(category)) {
            throw new ValidationException(record.l10n("条码[%s]非成品标签", code));
        }
    }

    public Map<String, Object> getResultMap(Records record, String code, String warehouseId, Double qty, Records material, Records line) {
        Map<String, Object> resultMap = new HashMap<>();
        Records unit = material.getRec("unit_id");
        resultMap.put("qty", qty);
        resultMap.put("sn", code);
        resultMap.put("material_id", material.getPresent());
        resultMap.put("material_name_spec", material.get("name_spec"));
        resultMap.put("warehouse_id", record.getEnv().get("md.warehouse", warehouseId).getPresent());
        resultMap.put("request_qty", line.getDouble("request_qty"));
        resultMap.put("return_qty", line.getDouble("return_qty"));
        resultMap.put("id", record.getId());
        resultMap.put("status", record.getString("status"));
        resultMap.put("stock_rule", material.getString("stock_rule"));
        resultMap.put("unit_id", unit.getPresent());
        resultMap.put("unit_accuracy", unit.get("accuracy"));
        return resultMap;
    }

    @ServiceMethod(label = "入库", doc = "根据退货明细生成入库单")
    public Object stockIn(Records records) {
        Environment env = records.getEnv();
        for (Records record : records) {
            Records salesReturnDetails = env.get("wms.sales_return_details")
                .find(Criteria.equal("return_id", record.getId()).and(Criteria.equal("status", "new")));
            if (!salesReturnDetails.any()) {
                throw new ValidationException("当前无可提交明细数据,请检查退货明细");
            }
            // 生成入库单
            Map<String, Object> stockInData = new HashMap<>();
            stockInData.put("type", "wms.sales_return");
            stockInData.put("related_code", record.getString("code"));
            stockInData.put("related_model", "wms.sales_return");
            stockInData.put("related_id", record.getId());
            stockInData.put("status", "new");
            Records stockIn = env.get("stock.stock_in").create(stockInData);
            salesReturnDetails.set("stock_in_id", stockIn.getId());
            salesReturnDetails.set("status", "to-stock");
            // 退货数据回写到出库的可建单数
            Records returnLine = env.get("wms.sales_return_line").find(Criteria.equal("return_id", record.getId()));
            for (Records line : returnLine) {
                Records salesOrder = line.getRec("so_id");
                if (salesOrder.any()) {
                    // 销售订单 存在
                    Records material = line.getRec("material_id");
                    Records salesOrderLind = env.get("sales.order_line").find(Criteria.equal("so_id", salesOrder.getId())
                        .and(Criteria.equal("material_id", material.getId())));
                    if (!salesOrderLind.any()) {
                        // 可能吗.应该不会存在,除非手动删除过,
                        continue;
                    }
                    double returnedQty = line.getDouble("return_qty");
                    salesOrderLind.set("uncommit_qty", Utils.round(salesOrderLind.getDouble("uncommit_qty") + returnedQty));
                    salesOrderLind.set("return_qty", Utils.round(salesOrderLind.getDouble("return_qty") + returnedQty));
                }
            }
        }
        String body = records.l10n("生成入库单");
        records.call("trackMessage", body);
        records.set("status", "done");
        return Action.reload(records.l10n("操作成功"));
    }
}
