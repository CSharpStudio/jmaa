package jmaa.modules.mfg.logistics.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author eric
 */
@Model.Meta(name = "mfg.product_storage_notice", label = "成品入库通知", inherit = {"code.auto_code", "mixin.order_status", "mixin.material"})
public class ProductStorageNotice extends Model {
    static Field code = Field.Char().label("单号").required(false);
    static Field remark = Field.Char().label("备注");
    static Field related_code = Field.Char().label("相关单据");
    static Field warehouse_id = Field.Many2one("md.warehouse").label("仓库").required();
    static Field material_id = Field.Many2one("md.material").label("产品编码").required().lookup(Criteria.equal("category", "finished"));
    static Field plan_qty = Field.Float().label("计划数量");
    static Field scan_qty = Field.Float().label("扫码数量").defaultValue(0d);
    static Field details_ids = Field.One2many("mfg.product_storage_notice_details", "product_storage_notice_id").label("入库明细");
    static Field status = Field.Selection(new Options() {{
        put("draft", "草稿");
        put("stocking", "执行中");
        put("done", "完成");
    }}).label("状态");

    @ServiceMethod(label = "扫描标签", doc = "扫码/确认,通过submit控制")
    public Object scanCode(Records record, String code, boolean submit) {
        // 仓库只管成品标签,和包装标签,packing.package
        // 成品标签存 lbl.material_label 表,  二维码就一个序列码,没分隔符
        Map<String, Object> result = new HashMap<>();
        Environment env = record.getEnv();
        Records materialLabel = env.get("lbl.material_label").find(Criteria.equal("sn", code));
        Records batchCode = null;
        //  batchCode = env.get("wip.batch_code") ;  这种直接报错了,用不了 batchCode.getMeta().isAuto()
        if (env.getRegistry().contains("wip.batch_code")) {
            batchCode = env.get("wip.batch_code");
        }
        Records mdPackage = env.get("packing.package");
        Records stockMaterial = record.getRec("material_id");
        double qty = 0d;
        String lotNum = null;
        Records material = null;
        String sn = null;
        if (materialLabel.any()) {
            material = materialLabel.getRec("material_id");
            // 物料配置过, 标签添加过,
            qty = materialLabel.getDouble("qty");
            lotNum = materialLabel.getString("lot_num");
        } else if (null != batchCode && (batchCode = batchCode.find(Criteria.equal("code", code))).any()) {
            // 批次标签
            material = batchCode.getRec("material_id");
            qty = batchCode.getDouble("qty");
            lotNum = code;
        } else if (null != mdPackage && (mdPackage = mdPackage.find(Criteria.equal("code", code))).any()) {
            qty = mdPackage.getDouble("package_qty");
            material = mdPackage.getRec("material_id");
            sn = code;
        } else {
            throw new ValidationException(record.l10n("条码[%s]非成品标签,无法识别", code));
        }
        checkLabel(record, code, stockMaterial, material, materialLabel, batchCode, mdPackage, qty);

        if (submit) {
            Map<String, Object> toCreate = new HashMap<>();
            toCreate.put("material_id", material.getId());
            toCreate.put("qty", qty);
            toCreate.put("warehouse_id", record.getRec("warehouse_id").getId());
            toCreate.put("status", "new");
            toCreate.put("product_storage_notice_id", record.getId());
            toCreate.put("lot_num", lotNum);
            toCreate.put("sn", sn);
            toCreate.put("label_id", materialLabel.getId());
            env.get("mfg.product_storage_notice_details").create(toCreate);
            record.set("scan_qty", Utils.round(record.getDouble("scan_qty") + qty));
            if ("draft".equals(record.getString("status"))) {
                record.set("status","stocking");
            }
            result.put("scan_qty", record.getDouble("scan_qty"));
            result.put("message", record.l10n("条码[%s]确认成功，入库数量[%s]", code,qty));
            return result;
        }
        result.put("data", getLabelData(record, code, qty, material));
        result.put("message", record.l10n("条码[%s]识别成功，待确认", code));
        return result;
    }

    public void checkLabel(Records record, String code, Records stockMaterial, Records material, Records materialLabel, Records batchCode, Records mdPackage, Double qty) {
        if (!stockMaterial.equals(material)) {
            throw new ValidationException(record.l10n("条码[%s]非当前单据所需物料", code));
        }
        // 查看当前标签是否已经被使用
        Records productStockInDetails = record.getEnv().get("mfg.product_storage_notice_details");
        if (materialLabel.any()) {
            productStockInDetails = productStockInDetails.find(Criteria.equal("label_id", materialLabel.getId()));
        }
        if (null != batchCode && batchCode.any()) {
            productStockInDetails = productStockInDetails.find(Criteria.equal("lot_num", code));
        }
        if (null != mdPackage && mdPackage.any()) {
            productStockInDetails = productStockInDetails.find(Criteria.equal("sn", mdPackage.getString("code")));
        }
        if (productStockInDetails.any()) {
            throw new ValidationException(record.l10n("条码[%s]已有记录，不能重复使用", code));
        }
        String category = material.getString("category");
        // "semi-finished", "半成品"  "finished", "成品"
        if (!"finished".equals(category)) {
            throw new ValidationException(record.l10n("条码[%s]非成品标签", code));
        }
        // 校验一下汇总的扫码数量,不能大于计划数量
        double planQty = record.getDouble("plan_qty");
        double scanQty = record.getDouble("scan_qty");
        scanQty = Utils.round(scanQty + qty);
        if (planQty < scanQty) {
            throw new ValidationException(record.l10n("累计扫码数量[%s](包含本次)大于计划数[%s],不能扫码", scanQty, planQty));
        }
    }

    public Map<String, Object> getLabelData(Records record, String code, double qty, Records material) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("qty", qty);
        resultMap.put("sn", code);
        resultMap.put("material_id", material.getPresent());
        resultMap.put("material_name_spec", material.get("name_spec"));
        resultMap.put("stock_rule",material.getString("stock_rule"));
        resultMap.put("abc_type",material.getString("abc_type"));
        return resultMap;
    }

    @ServiceMethod(label = "生成入库单", doc = "提交单据,完成,并生成入库单")
    public Object commit(Records records, Map<String, Object> values, @Doc(doc = "提交说明") String comment) {
        Environment env = records.getEnv();
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if ("done".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以提交", record.getSelection("status")));
            }
            Records productStockInDetails = record.getEnv().get("mfg.product_storage_notice_details")
                .find(Criteria.equal("product_storage_notice_id", record.getId()).and(Criteria.equal("status", "new")));
            if (!productStockInDetails.any()) {
                throw new ValidationException("当前无可提交明细数据,请检查入库明细");
            }
            // 生成入库单
            if (env.getRegistry().contains("stock.stock_in") && env.get("stock.stock_in").getMeta().isAuto()) {
                Map<String, Object> stockInData = new HashMap<>();
                stockInData.put("type", "mfg.product_storage_notice");
                stockInData.put("related_code", record.getString("code"));
                stockInData.put("related_model", "mfg.product_storage_notice");
                stockInData.put("related_id", record.getId());
                stockInData.put("status", "new");
                Records stockIn = record.getEnv().get("stock.stock_in").create(stockInData);
                productStockInDetails.set("stock_in_id", stockIn.getId());
            }
            productStockInDetails.set("status", "to-stock");
        }
        if (values != null) {
            records.update(values);
        }
        String body = records.l10n("提交,生成入库单") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        records.set("status", "done");
        return Action.success();
    }
}
