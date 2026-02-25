package jmaa.modules.inventory.models;

import org.jmaa.sdk.*;
import org.jmaa.sdk.core.Environment;
import org.jmaa.sdk.data.Cursor;
import org.jmaa.sdk.exceptions.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

@Model.Meta(inherit = "qsd.mrb")
public class Mrb extends Model {
    static Field type = Field.Selection().addSelection(new Options() {{
        put("qsd.mrb", "MRB");
    }}).label("检验类型");


    @Model.ServiceMethod(label = "审核", doc = "审核单据，从提交状态改为已审核")
    public Object approve(Records records, @Doc(doc = "审核意见") String comment) {
        for (Records record : records) {
            String orderStatus = record.getString("status");
            if (!"commit".equals(orderStatus)) {
                throw new ValidationException(records.l10n("当前状态为[%s]不可以审核", record.getSelection("status")));
            }
            // 先按照单条审核处理,后续如果有多条,就移上去
            String result = records.getString("result");
            String relatedModel = records.getString("related_model");
            if (!"iqc.sheet".equals(relatedModel)) {
                throw new ValidationException("非来料检验单据,无需挑选");
            }
            // 如果没有物料，那就直接完毕
            if (!records.getRec("related_id").getRec("details_ids").any()) {
                records.set("status", "done");
                return Action.reload(records.l10n("操作成功"));
            }
            // 如果没有仓库模块,那就下面的都不用执行,仓库模块包括了挑选,退供应商,入库,出库等等
            if ("concession".equals(result)) {
                // 让步接收, 就是全收,直接改入库单的明细状态为待入库
                mrbConcession(records);
            } else if ("return".equals(result)) {
                // 退货 直接退供应商
                // 获取当前mrb所有明细
                //1 修改对于入库单明细数据为完成,return_qty=qty
                //2. 检验单状态修改为已完成,不管是否退货,
                // 这种就是全退,
                mrbReturn(records);
            } else {
                // 挑选使用 走PDA挑选, 生成挑选单, 挑选单属于基础组件
                mrbPickOut(records, relatedModel);
                record.getRec("related_id").getRec("details_ids").set("status", "pick-out");
            }
        }
        String body = records.l10n("审核") + (Utils.isEmpty(comment) ? "" : ": " + comment);
        records.call("trackMessage", body);
        // 审核以后不驳回了, 单据关联太多, 要驳回,提交以后驳回
        records.set("status", "done");
        return Action.reload(records.l10n("操作成功"));
    }

    public static void mrbPickOut(Records records, String relatedModel) {
        String relatedCode = records.getString("related_code");
        Records iqcSheetRec = records.getRec("related_id");
        Records material = iqcSheetRec.getRec("material_id");
        Records pickOutRecord = records.getEnv().get("wms.pick_out");
        Map<String, Object> createMap = new HashMap<>();
        createMap.put("supplier_id", records.getRec("supplier_id").getId());
        createMap.put("related_code", relatedCode);
        createMap.put("type", relatedModel);
        createMap.put("material_id", material.getId());
        createMap.put("status", "draft");
        createMap.put("pick_type", "pick_stock");
        createMap.put("qty", records.get("qty"));
        pickOutRecord.create(createMap);
    }

    public void mrbReturn(Records records) {
        Environment env = records.getEnv();
        Records iqcSheet = records.getRec("related_id");
        double totalQty = records.getDouble("qty");
        // 需要退的物料,
        Records material = records.getRec("material_id");
        // 送检的物料明细
        String relatedCode = iqcSheet.getString("related_code");
        Records sheetDetails = iqcSheet.getRec("details_ids");
        // 来料检验
        // 关联单据是来料接收
        Records materialReceipt = env.get("wms.material_receipt").find(Criteria.equal("code", relatedCode));
        sheetDetails.set("status", "done");
        sheetDetails.stream().forEach(e -> e.set("return_qty", e.getDouble("qty")));
        createReturnSupplier(records, materialReceipt, totalQty, material, sheetDetails);
        // 处理入库单状态,明细全是done
        updateStockInStatus(sheetDetails.first().getRec("stock_in_id"));
        // 回写采购订单
        // computeReturnQty(records,totalQty);
    }

    public void computeReturnQty(Records purchaseOrderLine, double requestQty) {
        for (Records poLine : purchaseOrderLine) {
            // 当前采购订单明细的收货数量数量
            double receivedQty = poLine.getDouble("receive_qty");
            // 当前采购订单明细的退货数
            double returnedQty = poLine.getDouble("return_qty");
            // 入库数量
            double stockInQty = poLine.getDouble("stock_in_qty");
            //  比如: 送货2000 挑选退货1500, 那么有500 还没有真正入库,实际入库之前还有500 数量可操作,现在手动建单,如何控制   确定了,不控制
            double baseQty = Utils.round(receivedQty - returnedQty - stockInQty);
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

    public void updateStockInStatus(Records stockIn) {
        stockIn.flush();
        Cursor cr = stockIn.getEnv().getCursor();
        String sql = "select distinct status from stock_stock_in_details where stock_in_id=%s";
        for (Records row : stockIn) {
            cr.execute(sql, Arrays.asList(row.getId()));
            boolean done = cr.fetchAll().stream().map(r -> (String) r[0]).allMatch("done"::equals);
            if (done) {
                row.set("status", "done");
            } else {
                row.set("status", "stocking");
            }
        }
    }

    public void mrbConcession(Records records) {
        Records iqcSheet = records.getRec("related_id");
        Records detailsIds = iqcSheet.getRec("details_ids");
        detailsIds.set("status", "to-stock");
    }

    public void createReturnSupplier(Records record, Records materialReceipt, double totalQty, Records material, Records sheetDetails) {
        Environment env = record.getEnv();
        // 退货 直接退供应商
        Records returnSupplier = env.get("wms.return_supplier");
        Records returnSupplierLine = env.get("wms.return_supplier_line");
        Map<String, Object> createMap = new HashMap<>();
        createMap.put("supplier_id", materialReceipt.getRec("supplier_id").getId());
        createMap.put("return_date", new Date());
        createMap.put("company_id", env.getCompany().getId());
        createMap.put("related_code", record.get("code"));
        createMap.put("status", "commit");
        returnSupplier = returnSupplier.create(createMap);
        String returnId = returnSupplier.getId();
        // 处理采购订单号
        // 收料明细数据汇总
        Records materialReciptLine = materialReceipt.getRec("line_ids");
        Map<String, Double> poLineAndQtyMap = materialReciptLine.stream().filter(e ->
                Utils.large(e.getDouble("receive_qty"), 0d) && e.getRec("material_id").getId().equals(material.getId()))
            .collect(Collectors.toMap(e -> e.getRec("po_line_id").getId(), e -> e.getDouble("receive_qty")));
        Map<String, Object> poLineIdAndQtyMap = getPoLineIdAndQtyMap(record, totalQty, poLineAndQtyMap);
        Set<String> poLineSet = poLineIdAndQtyMap.keySet();
        createReturnSupplierLine(record, materialReciptLine, poLineSet, returnId, poLineIdAndQtyMap, returnSupplierLine);
        createReturnSupplierDetails(record, returnId, sheetDetails);
        returnSupplier.call("approve", "系统审核");
    }

    public void createReturnSupplierDetails(Records record, String returnId, Records sheetDetails) {
        Environment env = record.getEnv();
        Records returnSupplierDetails = env.get("wms.return_supplier_details");
        // 批次号处理的时候, 每个批次都要退,   挑选入库, 挑选部分数据,
        HashSet<String> ketSet = new HashSet<>(sheetDetails.getMeta().getFields().keySet());
        ketSet.remove("warehouse_id");
        for (Records sheetDetail : sheetDetails) {
            List<Map<String, Object>> returnSupplierDetailsRead = sheetDetail.read(ketSet);
            returnSupplierDetailsRead.forEach(e -> {
                e.put("return_id", returnId);
                e.put("qty", e.get("return_qty"));
            });
            returnSupplierDetails.createBatch(returnSupplierDetailsRead);
        }
    }

    public void createReturnSupplierLine(Records record, Records materialReciptLine, Set<String> poLineSet, String returnId, Map<String, Object> poLineIdAndQtyMap, Records returnSupplierLine) {
        Environment env = record.getEnv();
        Records mixinMaterialRecord = env.get("mixin.material");
        Set<String> keySet = mixinMaterialRecord.getMeta().getFields().keySet();
        HashSet<String> readField = new HashSet<>(keySet);
        readField.add("receive_qty");
        readField.add("po_line_id");
        readField.add("po_id");
        readField.remove("id");
        List<Map<String, Object>> read = materialReciptLine.read(readField).stream().filter(e -> poLineSet.contains(e.get("po_line_id"))).collect(Collectors.toList());
        if (read.isEmpty()) {
            throw new ValidationException(record.l10n("无法获取关联的收料物料明细数据,请检查"));
        } else {
            for (Map<String, Object> receiptLine : read) {
                receiptLine.put("return_id", returnId);
                receiptLine.put("status", "new");
                receiptLine.put("request_qty", poLineIdAndQtyMap.get(receiptLine.get("po_line_id")));
                receiptLine.put("return_qty", receiptLine.get("request_qty"));
            }
            returnSupplierLine.createBatch(read);
        }
    }

    public Map<String, Object> getPoLineIdAndQtyMap(Records records, double totalQty, Map<String, Double> poLineAndQtyMap) {
        Map<String, Object> insertMap = new HashMap<>();
        for (Map.Entry<String, Double> stringDoubleEntry : poLineAndQtyMap.entrySet()) {
            // 采购订单, 实收数量
            String key = stringDoubleEntry.getKey();
            Double value = stringDoubleEntry.getValue();
            double oldValue = Utils.toDouble(insertMap.get(key));
            if (Utils.large(totalQty, value)) {
                // 大于
                insertMap.put(key, Utils.round(oldValue + value));
            } else {
                // 小于
                insertMap.put(key, Utils.round(oldValue + totalQty));
            }
            totalQty = Utils.round(totalQty - value);
            if (Utils.lessOrEqual(totalQty, 0d)) {
                break;
            }
        }
        return insertMap;
    }
}
